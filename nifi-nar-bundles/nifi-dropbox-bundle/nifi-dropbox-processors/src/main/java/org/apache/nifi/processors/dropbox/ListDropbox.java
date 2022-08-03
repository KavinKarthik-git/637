/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.dropbox;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.PrimaryNodeOnly;
import org.apache.nifi.annotation.behavior.Stateful;
import org.apache.nifi.annotation.behavior.TriggerSerially;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.state.Scope;
import org.apache.nifi.context.PropertyContext;
import org.apache.nifi.dropbox.credentials.service.DropboxCredentialDetails;
import org.apache.nifi.dropbox.credentials.service.DropboxCredentialService;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processor.util.list.AbstractListProcessor;
import org.apache.nifi.processor.util.list.ListedEntityTracker;
import org.apache.nifi.serialization.record.RecordSchema;

@PrimaryNodeOnly
@TriggerSerially
@Tags({"dropbox", "storage"})
@CapabilityDescription("Retrieves a listing of files from Dropbox (shortcuts are ignored)." +
        " Each listed file may result in one FlowFile, the metadata being written as Flowfile attributes." +
        " When the 'Record Writer' property is set, the entire result is written as records to a single FlowFile." +
        " This Processor is designed to run on Primary Node only in a cluster. If the primary node changes, the new Primary Node will pick up where the" +
        " previous node left off without duplicating all of the data.")
@InputRequirement(Requirement.INPUT_FORBIDDEN)
@WritesAttributes({@WritesAttribute(attribute = DropboxFileInfo.ID, description = "The Dropbox identifier of the file"),
        @WritesAttribute(attribute = DropboxFileInfo.PATH, description = "The folder path where the file is located"),
        @WritesAttribute(attribute = DropboxFileInfo.FILENAME, description = "The name of the file"),
        @WritesAttribute(attribute = DropboxFileInfo.SIZE, description = "The size of the file"),
        @WritesAttribute(attribute = DropboxFileInfo.TIMESTAMP, description = "The server modified time, when the file was uploaded to Dropbox"),
        @WritesAttribute(attribute = DropboxFileInfo.REVISION, description = "Revision of the file")})
@Stateful(scopes = {Scope.CLUSTER}, description = "The processor stores necessary data to be able to keep track what files have been listed already. " +
        "What exactly needs to be stored depends on the 'Listing Strategy'.")
public class ListDropbox extends AbstractListProcessor<DropboxFileInfo> {

    public static final PropertyDescriptor FOLDER = new PropertyDescriptor.Builder()
            .name("folder")
            .displayName("Folder")
            .description("The Dropbox identifier or path of the folder from which to pull list of files." +
                    " 'Folder' should match the following regular expression pattern: /.*|id:.* ." +
                    " Example for folder identifier: id:odTlUvbpIEAAAAAAAAAGGQ." +
                    " Example for folder path: /Team1/Task1.")
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .required(true)
            .addValidator(StandardValidators.createRegexMatchingValidator(Pattern.compile("/.*|id:.*")))
            .defaultValue("/")
            .build();

    public static final PropertyDescriptor RECURSIVE_SEARCH = new PropertyDescriptor.Builder()
            .name("recursive-search")
            .displayName("Search Recursively")
            .description("Indicates whether to list files from subfolders of the Dropbox folder.")
            .required(true)
            .defaultValue("true")
            .allowableValues("true", "false")
            .build();

    public static final PropertyDescriptor MIN_AGE = new PropertyDescriptor.Builder()
            .name("min-age")
            .displayName("Minimum File Age")
            .description("The minimum age a file must be in order to be considered; any files newer than this will be ignored.")
            .required(true)
            .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)
            .defaultValue("0 sec")
            .build();

    public static final PropertyDescriptor CREDENTIAL_SERVICE = new PropertyDescriptor.Builder()
            .name("dropbox-credential-service")
            .displayName("Dropbox Credential Service")
            .description("Controller Service used to obtain Dropbox credentials (App Key, App Secret, Access Token, Refresh Token)." +
                    " See controller service's Additional Details for more information.")
            .identifiesControllerService(DropboxCredentialService.class)
            .required(true)
            .build();

    public static final PropertyDescriptor LISTING_STRATEGY = new PropertyDescriptor.Builder()
            .fromPropertyDescriptor(AbstractListProcessor.LISTING_STRATEGY)
            .allowableValues(BY_TIMESTAMPS, BY_ENTITIES, BY_TIME_WINDOW, NO_TRACKING)
            .build();

    public static final PropertyDescriptor TRACKING_STATE_CACHE = new PropertyDescriptor.Builder()
            .fromPropertyDescriptor(ListedEntityTracker.TRACKING_STATE_CACHE)
            .dependsOn(LISTING_STRATEGY, BY_ENTITIES)
            .build();

    public static final PropertyDescriptor TRACKING_TIME_WINDOW = new PropertyDescriptor.Builder()
            .fromPropertyDescriptor(ListedEntityTracker.TRACKING_TIME_WINDOW)
            .dependsOn(LISTING_STRATEGY, BY_ENTITIES)
            .build();

    public static final PropertyDescriptor INITIAL_LISTING_TARGET = new PropertyDescriptor.Builder()
            .fromPropertyDescriptor(ListedEntityTracker.INITIAL_LISTING_TARGET)
            .dependsOn(LISTING_STRATEGY, BY_ENTITIES)
            .build();

    private static final List<PropertyDescriptor> PROPERTIES = Collections.unmodifiableList(Arrays.asList(
            CREDENTIAL_SERVICE,
            FOLDER,
            RECURSIVE_SEARCH,
            MIN_AGE,
            LISTING_STRATEGY,
            TRACKING_STATE_CACHE,
            TRACKING_TIME_WINDOW,
            INITIAL_LISTING_TARGET,
            RECORD_WRITER
    ));

    private DbxClientV2 dropboxApiClient;

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        dropboxApiClient = getDropboxApiClient(context);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTIES;
    }

    @Override
    protected Map<String, String> createAttributes(
            final DropboxFileInfo entity,
            final ProcessContext context) {
        final Map<String, String> attributes = new HashMap<>();

        for (DropboxFlowFileAttribute attribute : DropboxFlowFileAttribute.values()) {
            Optional.ofNullable(attribute.getValue(entity))
                    .ifPresent(value -> attributes.put(attribute.getName(), value));
        }

        return attributes;
    }

    @Override
    protected String getPath(final ProcessContext context) {
        return context.getProperty(FOLDER).evaluateAttributeExpressions().getValue();
    }

    protected DbxClientV2 getDropboxApiClient(ProcessContext context) {
        final DropboxCredentialService credentialService = context.getProperty(CREDENTIAL_SERVICE)
                .asControllerService(DropboxCredentialService.class);
        DbxRequestConfig config = new DbxRequestConfig(format("%s-%s", getClass().getSimpleName(), getIdentifier()));
        DropboxCredentialDetails credential = credentialService.getDropboxCredential();
        return new DbxClientV2(config, new DbxCredential(credential.getAccessToken(), -1L,
                credential.getRefreshToken(), credential.getAppKey(), credential.getAppSecret()));
    }

    @Override
    protected List<DropboxFileInfo> performListing(ProcessContext context, Long minTimestamp,
            ListingMode listingMode) throws IOException {
        final List<DropboxFileInfo> listing = new ArrayList<>();

        final String folderName = getPath(context);
        final Boolean recursive = context.getProperty(RECURSIVE_SEARCH).asBoolean();
        final Long minAge = context.getProperty(MIN_AGE).asTimePeriod(TimeUnit.MILLISECONDS);

        try {
            Predicate<FileMetadata> metadataFilter = createMetadataFilter(minTimestamp, minAge);

            ListFolderBuilder listFolderBuilder = dropboxApiClient.files().listFolderBuilder(convertFolderName(folderName));
            ListFolderResult result = listFolderBuilder
                    .withRecursive(recursive)
                    .start();

            List<FileMetadata> metadataList = new ArrayList<>(filterMetadata(result, metadataFilter));

            while (result.getHasMore()) {
                result = dropboxApiClient.files().listFolderContinue(result.getCursor());
                metadataList.addAll(filterMetadata(result, metadataFilter));
            }

            for (FileMetadata metadata : metadataList) {
                DropboxFileInfo.Builder builder = new DropboxFileInfo.Builder()
                        .id(metadata.getId())
                        .path(getParentPath(metadata.getPathDisplay()))
                        .name(metadata.getName())
                        .size(metadata.getSize())
                        .timestamp(metadata.getServerModified().getTime())
                        .revision(metadata.getRev());

                listing.add(builder.build());
            }
        } catch (DbxException e) {
            throw new IOException("Failed to list Dropbox folder [" + folderName + "]", e);
        }

        return listing;
    }

    @Override
    protected boolean isListingResetNecessary(final PropertyDescriptor property) {
        return LISTING_STRATEGY.equals(property)
                || FOLDER.equals(property)
                || RECURSIVE_SEARCH.equals(property);
    }

    @Override
    protected Scope getStateScope(final PropertyContext context) {
        return Scope.CLUSTER;
    }

    @Override
    protected RecordSchema getRecordSchema() {
        return DropboxFileInfo.getRecordSchema();
    }

    @Override
    protected Integer countUnfilteredListing(final ProcessContext context) throws IOException {
        return performListing(context, null, ListingMode.CONFIGURATION_VERIFICATION).size();
    }

    @Override
    protected String getListingContainerName(final ProcessContext context) {
        return format("Dropbox Folder [%s]", getPath(context));
    }

    private Predicate<FileMetadata> createMetadataFilter(Long minTimestamp, Long minAge) {
        Predicate<FileMetadata> metadataFilter = FileMetadata::getIsDownloadable;

        if (minTimestamp != null && minTimestamp > 0) {
            metadataFilter = metadataFilter.and(metadata -> metadata.getServerModified().getTime() >= minTimestamp);
        }

        if (minAge != null && minAge > 0) {
            long maxTimestamp = System.currentTimeMillis() - minAge;
            metadataFilter = metadataFilter.and(metadata -> metadata.getServerModified().getTime() < maxTimestamp);
        }
        return metadataFilter;
    }

    private List<FileMetadata> filterMetadata(ListFolderResult result, Predicate<FileMetadata> metadataFilter) {
        return result.getEntries().stream()
                .filter(metadata -> metadata instanceof FileMetadata)
                .map(FileMetadata.class::cast)
                .filter(metadataFilter)
                .collect(toList());
    }

    private String getParentPath(String fullPath) {
        int idx = fullPath.lastIndexOf("/");
        String parentPath = fullPath.substring(0, idx);
        return "".equals(parentPath) ? "/" : parentPath;
    }

    private String convertFolderName(String folderName) {
        return "/".equals(folderName) ? "" : folderName;
    }
}

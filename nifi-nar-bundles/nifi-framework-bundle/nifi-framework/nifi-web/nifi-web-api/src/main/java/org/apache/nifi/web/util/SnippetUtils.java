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
package org.apache.nifi.web.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.authorization.AccessPolicy;
import org.apache.nifi.authorization.RequestAction;
import org.apache.nifi.authorization.Resource;
import org.apache.nifi.authorization.resource.ResourceFactory;
import org.apache.nifi.authorization.resource.ResourceType;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.connectable.ConnectableType;
import org.apache.nifi.connectable.Connection;
import org.apache.nifi.connectable.Funnel;
import org.apache.nifi.connectable.Port;
import org.apache.nifi.controller.FlowController;
import org.apache.nifi.controller.ProcessorNode;
import org.apache.nifi.controller.ScheduledState;
import org.apache.nifi.controller.Snippet;
import org.apache.nifi.controller.label.Label;
import org.apache.nifi.controller.service.ControllerServiceNode;
import org.apache.nifi.controller.service.ControllerServiceState;
import org.apache.nifi.groups.ProcessGroup;
import org.apache.nifi.groups.RemoteProcessGroup;
import org.apache.nifi.util.TypeOneUUIDGenerator;
import org.apache.nifi.web.api.dto.AccessPolicyDTO;
import org.apache.nifi.web.api.dto.ConnectableDTO;
import org.apache.nifi.web.api.dto.ConnectionDTO;
import org.apache.nifi.web.api.dto.ControllerServiceDTO;
import org.apache.nifi.web.api.dto.DtoFactory;
import org.apache.nifi.web.api.dto.FlowSnippetDTO;
import org.apache.nifi.web.api.dto.FunnelDTO;
import org.apache.nifi.web.api.dto.LabelDTO;
import org.apache.nifi.web.api.dto.PortDTO;
import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.apache.nifi.web.api.dto.ProcessorConfigDTO;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.apache.nifi.web.api.dto.PropertyDescriptorDTO;
import org.apache.nifi.web.api.dto.RemoteProcessGroupContentsDTO;
import org.apache.nifi.web.api.dto.RemoteProcessGroupDTO;
import org.apache.nifi.web.api.dto.RemoteProcessGroupPortDTO;
import org.apache.nifi.web.api.entity.TenantEntity;
import org.apache.nifi.web.dao.AccessPolicyDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Template utilities.
 */
public final class SnippetUtils {

    private static final Logger logger = LoggerFactory.getLogger(SnippetUtils.class);

    private FlowController flowController;
    private DtoFactory dtoFactory;
    private AccessPolicyDAO accessPolicyDAO;


    /**
     * Populates the specified snippet and returns the details.
     *
     * @param snippet snippet
     * @param recurse recurse
     * @param includeControllerServices whether or not to include controller services in the flow snippet dto
     * @return snippet
     */
    public FlowSnippetDTO populateFlowSnippet(final Snippet snippet, final boolean recurse, final boolean includeControllerServices, boolean removeInstanceId) {
        final FlowSnippetDTO snippetDto = new FlowSnippetDTO(removeInstanceId);
        final String groupId = snippet.getParentGroupId();
        final ProcessGroup processGroup = flowController.getGroup(groupId);

        // ensure the group could be found
        if (processGroup == null) {
            throw new IllegalStateException("The parent process group for this snippet could not be found.");
        }

        final Set<ControllerServiceDTO> controllerServices = new HashSet<>();

        // add any processors
        if (!snippet.getProcessors().isEmpty()) {
            final Set<ProcessorDTO> processors = new LinkedHashSet<>();
            for (final String processorId : snippet.getProcessors().keySet()) {
                final ProcessorNode processor = processGroup.getProcessor(processorId);
                if (processor == null) {
                    throw new IllegalStateException("A processor in this snippet could not be found.");
                }
                processors.add(dtoFactory.createProcessorDto(processor));

                if (includeControllerServices) {
                    controllerServices.addAll(getControllerServices(processor.getProperties()));
                }
            }
            this.normalizeCoordinates(processors);
            snippetDto.setProcessors(processors);
        }



        // add any connections
        if (!snippet.getConnections().isEmpty()) {
            final Set<ConnectionDTO> connections = new LinkedHashSet<>();
            for (final String connectionId : snippet.getConnections().keySet()) {
                final Connection connection = processGroup.getConnection(connectionId);
                if (connection == null) {
                    throw new IllegalStateException("A connection in this snippet could not be found.");
                }
                connections.add(dtoFactory.createConnectionDto(connection));
            }
            snippetDto.setConnections(connections);
        }

        // add any funnels
        if (!snippet.getFunnels().isEmpty()) {
            final Set<FunnelDTO> funnels = new LinkedHashSet<>();
            for (final String funnelId : snippet.getFunnels().keySet()) {
                final Funnel funnel = processGroup.getFunnel(funnelId);
                if (funnel == null) {
                    throw new IllegalStateException("A funnel in this snippet could not be found.");
                }
                funnels.add(dtoFactory.createFunnelDto(funnel));
            }
            snippetDto.setFunnels(funnels);
        }

        // add any input ports
        if (!snippet.getInputPorts().isEmpty()) {
            final Set<PortDTO> inputPorts = new LinkedHashSet<>();
            for (final String inputPortId : snippet.getInputPorts().keySet()) {
                final Port inputPort = processGroup.getInputPort(inputPortId);
                if (inputPort == null) {
                    throw new IllegalStateException("An input port in this snippet could not be found.");
                }
                inputPorts.add(dtoFactory.createPortDto(inputPort));
            }
            snippetDto.setInputPorts(inputPorts);
        }

        // add any labels
        if (!snippet.getLabels().isEmpty()) {
            final Set<LabelDTO> labels = new LinkedHashSet<>();
            for (final String labelId : snippet.getLabels().keySet()) {
                final Label label = processGroup.getLabel(labelId);
                if (label == null) {
                    throw new IllegalStateException("A label in this snippet could not be found.");
                }
                labels.add(dtoFactory.createLabelDto(label));
            }
            snippetDto.setLabels(labels);
        }

        // add any output ports
        if (!snippet.getOutputPorts().isEmpty()) {
            final Set<PortDTO> outputPorts = new LinkedHashSet<>();
            for (final String outputPortId : snippet.getOutputPorts().keySet()) {
                final Port outputPort = processGroup.getOutputPort(outputPortId);
                if (outputPort == null) {
                    throw new IllegalStateException("An output port in this snippet could not be found.");
                }
                outputPorts.add(dtoFactory.createPortDto(outputPort));
            }
            snippetDto.setOutputPorts(outputPorts);
        }

        // add any process groups
        if (!snippet.getProcessGroups().isEmpty()) {
            final Set<ProcessGroupDTO> processGroups = new LinkedHashSet<>();
            for (final String childGroupId : snippet.getProcessGroups().keySet()) {
                final ProcessGroup childGroup = processGroup.getProcessGroup(childGroupId);
                if (childGroup == null) {
                    throw new IllegalStateException("A process group in this snippet could not be found.");
                }

                final ProcessGroupDTO childGroupDto = dtoFactory.createProcessGroupDto(childGroup, recurse);
                processGroups.add(childGroupDto);

                addControllerServices(childGroup, childGroupDto);
            }
            snippetDto.setProcessGroups(processGroups);
        }

        // add any remote process groups
        if (!snippet.getRemoteProcessGroups().isEmpty()) {
            final Set<RemoteProcessGroupDTO> remoteProcessGroups = new LinkedHashSet<>();
            for (final String remoteProcessGroupId : snippet.getRemoteProcessGroups().keySet()) {
                final RemoteProcessGroup remoteProcessGroup = processGroup.getRemoteProcessGroup(remoteProcessGroupId);
                if (remoteProcessGroup == null) {
                    throw new IllegalStateException("A remote process group in this snippet could not be found.");
                }
                remoteProcessGroups.add(dtoFactory.createRemoteProcessGroupDto(remoteProcessGroup));
            }
            snippetDto.setRemoteProcessGroups(remoteProcessGroups);
        }

        snippetDto.setControllerServices(controllerServices);

        return snippetDto;
    }

    private void addControllerServices(final ProcessGroup group, final ProcessGroupDTO dto) {
        final FlowSnippetDTO contents = dto.getContents();
        if (contents == null) {
            return;
        }

        final Set<ControllerServiceDTO> controllerServices = new HashSet<>();

        for (final ProcessorNode procNode : group.getProcessors()) {
            final Set<ControllerServiceDTO> servicesForProcessor = getControllerServices(procNode.getProperties());
            controllerServices.addAll(servicesForProcessor);
        }

        contents.setControllerServices(controllerServices);

        // Map child process group ID to the child process group for easy lookup
        final Map<String, ProcessGroupDTO> childGroupMap = contents.getProcessGroups().stream()
            .collect(Collectors.toMap(childGroupDto -> childGroupDto.getId(), childGroupDto -> childGroupDto));

        for (final ProcessGroup childGroup : group.getProcessGroups()) {
            final ProcessGroupDTO childDto = childGroupMap.get(childGroup.getIdentifier());
            if (childDto == null) {
                continue;
            }

            addControllerServices(childGroup, childDto);
        }
    }

    private Set<ControllerServiceDTO> getControllerServices(final Map<PropertyDescriptor, String> componentProperties) {
        final Set<ControllerServiceDTO> serviceDtos = new HashSet<>();

        for (final Map.Entry<PropertyDescriptor, String> entry : componentProperties.entrySet()) {
            final PropertyDescriptor descriptor = entry.getKey();
            if (descriptor.getControllerServiceDefinition() != null) {
                final String controllerServiceId = entry.getValue();
                if (controllerServiceId != null) {
                    final ControllerServiceNode serviceNode = flowController.getControllerServiceNode(controllerServiceId);
                    if (serviceNode != null) {
                        serviceDtos.add(dtoFactory.createControllerServiceDto(serviceNode));

                        final Set<ControllerServiceDTO> recursiveRefs = getControllerServices(serviceNode.getProperties());
                        serviceDtos.addAll(recursiveRefs);
                    }
                }
            }
        }

        return serviceDtos;
    }


    public FlowSnippetDTO copy(final FlowSnippetDTO snippetContents, final ProcessGroup group, final String idGenerationSeed, boolean isCopy) {
        final FlowSnippetDTO snippetCopy = copyContentsForGroup(snippetContents, group.getIdentifier(), null, null, idGenerationSeed, isCopy);
        resolveNameConflicts(snippetCopy, group);
        return snippetCopy;
    }

    private void resolveNameConflicts(final FlowSnippetDTO snippetContents, final ProcessGroup group) {
        // get a list of all names of ports so that we can rename the ports as needed.
        final List<String> existingPortNames = new ArrayList<>();
        for (final Port inputPort : group.getInputPorts()) {
            existingPortNames.add(inputPort.getName());
        }
        for (final Port outputPort : group.getOutputPorts()) {
            existingPortNames.add(outputPort.getName());
        }

        // rename ports
        if (snippetContents.getInputPorts() != null) {
            for (final PortDTO portDTO : snippetContents.getInputPorts()) {
                String portName = portDTO.getName();
                while (existingPortNames.contains(portName)) {
                    portName = "Copy of " + portName;
                }
                portDTO.setName(portName);
                existingPortNames.add(portDTO.getName());
            }
        }
        if (snippetContents.getOutputPorts() != null) {
            for (final PortDTO portDTO : snippetContents.getOutputPorts()) {
                String portName = portDTO.getName();
                while (existingPortNames.contains(portName)) {
                    portName = "Copy of " + portName;
                }
                portDTO.setName(portName);
                existingPortNames.add(portDTO.getName());
            }
        }

        // get a list of all names of process groups so that we can rename as needed.
        final List<String> groupNames = new ArrayList<>();
        for (final ProcessGroup childGroup : group.getProcessGroups()) {
            groupNames.add(childGroup.getName());
        }

        if (snippetContents.getProcessGroups() != null) {
            for (final ProcessGroupDTO groupDTO : snippetContents.getProcessGroups()) {
                String groupName = groupDTO.getName();
                while (groupNames.contains(groupName)) {
                    groupName = "Copy of " + groupName;
                }
                groupDTO.setName(groupName);
                groupNames.add(groupDTO.getName());
            }
        }
    }

    private FlowSnippetDTO copyContentsForGroup(final FlowSnippetDTO snippetContents, final String groupId, final Map<String, ConnectableDTO> parentConnectableMap,
                                                Map<String, String> serviceIdMap, final String idGenerationSeed, boolean isCopy) {

        final FlowSnippetDTO snippetContentsCopy = new FlowSnippetDTO();
        try {
            //
            // Copy the Controller Services
            //
            if (serviceIdMap == null) {
                serviceIdMap = new HashMap<>();
            }

            final Set<ControllerServiceDTO> services = new HashSet<>();
            if (snippetContents.getControllerServices() != null) {
                for (final ControllerServiceDTO serviceDTO : snippetContents.getControllerServices()) {
                    final ControllerServiceDTO service = dtoFactory.copy(serviceDTO);
                    service.setId(generateId(serviceDTO.getId(), idGenerationSeed, isCopy));
                    service.setState(ControllerServiceState.DISABLED.name());
                    services.add(service);

                    // Map old service ID to new service ID so that we can make sure that we reference the new ones.
                    serviceIdMap.put(serviceDTO.getId(), service.getId());

                    // clone policies as appropriate
                    if (isCopy) {
                        cloneComponentSpecificPolicies(
                                ResourceFactory.getComponentResource(ResourceType.ControllerService, serviceDTO.getId(), serviceDTO.getName()),
                                ResourceFactory.getComponentResource(ResourceType.ControllerService, service.getId(), service.getName()), idGenerationSeed);
                    }
                }
            }

            // if there is any controller service that maps to another controller service, update the id's
            for (final ControllerServiceDTO serviceDTO : services) {
                final Map<String, String> properties = serviceDTO.getProperties();
                final Map<String, PropertyDescriptorDTO> descriptors = serviceDTO.getDescriptors();
                if (properties != null && descriptors != null) {
                    for (final PropertyDescriptorDTO descriptor : descriptors.values()) {
                        if (descriptor.getIdentifiesControllerService() != null) {
                            final String currentServiceId = properties.get(descriptor.getName());
                            if (currentServiceId == null) {
                                continue;
                            }

                            final String newServiceId = serviceIdMap.get(currentServiceId);
                            properties.put(descriptor.getName(), newServiceId);
                        }
                    }
                }
            }
            snippetContentsCopy.setControllerServices(services);

            //
            // Copy the labels
            //
            final Set<LabelDTO> labels = new HashSet<>();
            if (snippetContents.getLabels() != null) {
                for (final LabelDTO labelDTO : snippetContents.getLabels()) {
                    final LabelDTO label = dtoFactory.copy(labelDTO);
                    label.setId(generateId(labelDTO.getId(), idGenerationSeed, isCopy));
                    label.setParentGroupId(groupId);
                    labels.add(label);

                    // clone policies as appropriate
                    if (isCopy) {
                        cloneComponentSpecificPolicies(
                                ResourceFactory.getComponentResource(ResourceType.Label, labelDTO.getId(), labelDTO.getLabel()),
                                ResourceFactory.getComponentResource(ResourceType.Label, label.getId(), label.getLabel()), idGenerationSeed);
                    }
                }
            }
            snippetContentsCopy.setLabels(labels);

            //
            // Copy connectable components
            //
            // maps a group ID-ID of a Connectable in the template to the new instance
            final Map<String, ConnectableDTO> connectableMap = new HashMap<>();

            //
            // Copy the funnels
            //
            final Set<FunnelDTO> funnels = new HashSet<>();
            if (snippetContents.getFunnels() != null) {
                for (final FunnelDTO funnelDTO : snippetContents.getFunnels()) {
                    final FunnelDTO cp = dtoFactory.copy(funnelDTO);
                    cp.setId(generateId(funnelDTO.getId(), idGenerationSeed, isCopy));
                    cp.setParentGroupId(groupId);
                    funnels.add(cp);

                    connectableMap.put(funnelDTO.getParentGroupId() + "-" + funnelDTO.getId(), dtoFactory.createConnectableDto(cp));

                    // clone policies as appropriate
                    if (isCopy) {
                        cloneComponentSpecificPolicies(
                                ResourceFactory.getComponentResource(ResourceType.Funnel, funnelDTO.getId(), funnelDTO.getId()),
                                ResourceFactory.getComponentResource(ResourceType.Funnel, cp.getId(), cp.getId()), idGenerationSeed);
                    }
                }
            }
            snippetContentsCopy.setFunnels(funnels);

            final Set<PortDTO> inputPorts = new HashSet<>();
            if (snippetContents.getInputPorts() != null) {
                for (final PortDTO portDTO : snippetContents.getInputPorts()) {
                    final PortDTO cp = dtoFactory.copy(portDTO);
                    cp.setId(generateId(portDTO.getId(), idGenerationSeed, isCopy));
                    cp.setParentGroupId(groupId);
                    cp.setState(ScheduledState.STOPPED.toString());
                    inputPorts.add(cp);

                    final ConnectableDTO portConnectable = dtoFactory.createConnectableDto(cp, ConnectableType.INPUT_PORT);
                    connectableMap.put(portDTO.getParentGroupId() + "-" + portDTO.getId(), portConnectable);
                    if (parentConnectableMap != null) {
                        parentConnectableMap.put(portDTO.getParentGroupId() + "-" + portDTO.getId(), portConnectable);
                    }

                    // clone policies as appropriate
                    if (isCopy) {
                        cloneComponentSpecificPolicies(
                                ResourceFactory.getComponentResource(ResourceType.InputPort, portDTO.getId(), portDTO.getName()),
                                ResourceFactory.getComponentResource(ResourceType.InputPort, cp.getId(), cp.getName()), idGenerationSeed);
                    }
                }
            }
            snippetContentsCopy.setInputPorts(inputPorts);

            final Set<PortDTO> outputPorts = new HashSet<>();
            if (snippetContents.getOutputPorts() != null) {
                for (final PortDTO portDTO : snippetContents.getOutputPorts()) {
                    final PortDTO cp = dtoFactory.copy(portDTO);
                    cp.setId(generateId(portDTO.getId(), idGenerationSeed, isCopy));
                    cp.setParentGroupId(groupId);
                    cp.setState(ScheduledState.STOPPED.toString());
                    outputPorts.add(cp);

                    final ConnectableDTO portConnectable = dtoFactory.createConnectableDto(cp, ConnectableType.OUTPUT_PORT);
                    connectableMap.put(portDTO.getParentGroupId() + "-" + portDTO.getId(), portConnectable);
                    if (parentConnectableMap != null) {
                        parentConnectableMap.put(portDTO.getParentGroupId() + "-" + portDTO.getId(), portConnectable);
                    }

                    // clone policies as appropriate
                    if (isCopy) {
                        cloneComponentSpecificPolicies(
                                ResourceFactory.getComponentResource(ResourceType.OutputPort, portDTO.getId(), portDTO.getName()),
                                ResourceFactory.getComponentResource(ResourceType.OutputPort, cp.getId(), cp.getName()), idGenerationSeed);
                    }
                }
            }
            snippetContentsCopy.setOutputPorts(outputPorts);

            //
            // Copy the processors
            //
            final Set<ProcessorDTO> processors = new HashSet<>();
            if (snippetContents.getProcessors() != null) {
                for (final ProcessorDTO processorDTO : snippetContents.getProcessors()) {
                    final ProcessorDTO cp = dtoFactory.copy(processorDTO);
                    cp.setId(generateId(processorDTO.getId(), idGenerationSeed, isCopy));
                    cp.setParentGroupId(groupId);
                    cp.setState(ScheduledState.STOPPED.toString());
                    processors.add(cp);

                    connectableMap.put(processorDTO.getParentGroupId() + "-" + processorDTO.getId(), dtoFactory.createConnectableDto(cp));

                    // clone policies as appropriate
                    if (isCopy) {
                        cloneComponentSpecificPolicies(
                                ResourceFactory.getComponentResource(ResourceType.Processor, processorDTO.getId(), processorDTO.getName()),
                                ResourceFactory.getComponentResource(ResourceType.Processor, cp.getId(), cp.getName()), idGenerationSeed);
                    }
                }
            }
            snippetContentsCopy.setProcessors(processors);

            // if there is any controller service that maps to another controller service, update the id's
            updateControllerServiceIdentifiers(snippetContentsCopy, serviceIdMap);

            //
            // Copy ProcessGroups
            //
            // instantiate the process groups, renaming as necessary
            final Set<ProcessGroupDTO> groups = new HashSet<>();
            if (snippetContents.getProcessGroups() != null) {
                for (final ProcessGroupDTO groupDTO : snippetContents.getProcessGroups()) {
                    final ProcessGroupDTO cp = dtoFactory.copy(groupDTO, false);
                    cp.setId(generateId(groupDTO.getId(), idGenerationSeed, isCopy));
                    cp.setParentGroupId(groupId);

                    // copy the contents of this group - we do not copy via the dto factory since we want to specify new ids
                    final FlowSnippetDTO contentsCopy = copyContentsForGroup(groupDTO.getContents(), cp.getId(), connectableMap, serviceIdMap, idGenerationSeed, isCopy);
                    cp.setContents(contentsCopy);
                    groups.add(cp);

                    // clone policies as appropriate
                    if (isCopy) {
                        cloneComponentSpecificPolicies(
                                ResourceFactory.getComponentResource(ResourceType.ProcessGroup, groupDTO.getId(), groupDTO.getName()),
                                ResourceFactory.getComponentResource(ResourceType.ProcessGroup, cp.getId(), cp.getName()), idGenerationSeed);
                    }
                }
            }
            snippetContentsCopy.setProcessGroups(groups);

            final Set<RemoteProcessGroupDTO> remoteGroups = new HashSet<>();
            if (snippetContents.getRemoteProcessGroups() != null) {
                for (final RemoteProcessGroupDTO remoteGroupDTO : snippetContents.getRemoteProcessGroups()) {
                    final RemoteProcessGroupDTO cp = dtoFactory.copy(remoteGroupDTO);
                    cp.setId(generateId(remoteGroupDTO.getId(), idGenerationSeed, isCopy));
                    cp.setParentGroupId(groupId);

                    final RemoteProcessGroupContentsDTO contents = cp.getContents();
                    if (contents != null && contents.getInputPorts() != null) {
                        for (final RemoteProcessGroupPortDTO remotePort : contents.getInputPorts()) {
                            remotePort.setGroupId(cp.getId());
                            connectableMap.put(remoteGroupDTO.getId() + "-" + remotePort.getId(), dtoFactory.createConnectableDto(remotePort, ConnectableType.REMOTE_INPUT_PORT));
                        }
                    }
                    if (contents != null && contents.getOutputPorts() != null) {
                        for (final RemoteProcessGroupPortDTO remotePort : contents.getOutputPorts()) {
                            remotePort.setGroupId(cp.getId());
                            connectableMap.put(remoteGroupDTO.getId() + "-" + remotePort.getId(), dtoFactory.createConnectableDto(remotePort, ConnectableType.REMOTE_OUTPUT_PORT));
                        }
                    }

                    remoteGroups.add(cp);

                    // clone policies as appropriate
                    if (isCopy) {
                        cloneComponentSpecificPolicies(
                                ResourceFactory.getComponentResource(ResourceType.RemoteProcessGroup, remoteGroupDTO.getId(), remoteGroupDTO.getName()),
                                ResourceFactory.getComponentResource(ResourceType.RemoteProcessGroup, cp.getId(), cp.getName()), idGenerationSeed);
                    }
                }
            }
            snippetContentsCopy.setRemoteProcessGroups(remoteGroups);

            final Set<ConnectionDTO> connections = new HashSet<>();
            if (snippetContents.getConnections() != null) {
                for (final ConnectionDTO connectionDTO : snippetContents.getConnections()) {
                    final ConnectionDTO cp = dtoFactory.copy(connectionDTO);

                    final ConnectableDTO source = connectableMap.get(cp.getSource().getGroupId() + "-" + cp.getSource().getId());
                    final ConnectableDTO destination = connectableMap.get(cp.getDestination().getGroupId() + "-" + cp.getDestination().getId());

                    // ensure all referenced components are present
                    if (source == null || destination == null) {
                        throw new IllegalArgumentException("The flow snippet contains a Connection that references a component that is not included.");
                    }

                    cp.setId(generateId(connectionDTO.getId(), idGenerationSeed, isCopy));
                    cp.setSource(source);
                    cp.setDestination(destination);
                    cp.setParentGroupId(groupId);
                    connections.add(cp);

                    // note - no need to copy policies of a connection as their permissions are inferred through the source and destination
                }
            }
            snippetContentsCopy.setConnections(connections);

            return snippetContentsCopy;
        } catch (Exception e) {
            // attempt to role back any policies of the copies that were created in preparation for the clone
            rollbackClonedPolicies(snippetContentsCopy);

            // rethrow the original exception
            throw e;
        }
    }

    /**
     * Clones all the component specified policies for the specified original component. This will include the component resource, data resource
     * for the component, data transfer resource for the component, and policy resource for the component.
     *
     * @param originalComponentResource original component resource
     * @param clonedComponentResource cloned component resource
     * @param idGenerationSeed id generation seed
     */
    private void cloneComponentSpecificPolicies(final Resource originalComponentResource, final Resource clonedComponentResource, final String idGenerationSeed) {
        final Map<Resource, Resource> resources = new HashMap<>();
        resources.put(originalComponentResource, clonedComponentResource);
        resources.put(ResourceFactory.getDataResource(originalComponentResource), ResourceFactory.getDataResource(clonedComponentResource));
        resources.put(ResourceFactory.getDataTransferResource(originalComponentResource), ResourceFactory.getDataTransferResource(clonedComponentResource));
        resources.put(ResourceFactory.getPolicyResource(originalComponentResource), ResourceFactory.getPolicyResource(clonedComponentResource));

        for (final Entry<Resource, Resource> entry : resources.entrySet()) {
            final Resource originalResource = entry.getKey();
            final Resource cloneResource = entry.getValue();

            for (final RequestAction action : RequestAction.values()) {
                final AccessPolicy accessPolicy = accessPolicyDAO.getAccessPolicy(action, originalResource.getIdentifier());

                // if there is a component specific policy we want to clone it for the new component
                if (accessPolicy != null) {
                    final AccessPolicyDTO cloneAccessPolicy = new AccessPolicyDTO();
                    cloneAccessPolicy.setId(generateId(accessPolicy.getIdentifier(), idGenerationSeed, true));
                    cloneAccessPolicy.setAction(accessPolicy.getAction().toString());
                    cloneAccessPolicy.setResource(cloneResource.getIdentifier());

                    final Set<TenantEntity> users = new HashSet<>();
                    accessPolicy.getUsers().forEach(userId -> {
                        final TenantEntity entity = new TenantEntity();
                        entity.setId(userId);
                        users.add(entity);
                    });
                    cloneAccessPolicy.setUsers(users);

                    final Set<TenantEntity> groups = new HashSet<>();
                    accessPolicy.getGroups().forEach(groupId -> {
                        final TenantEntity entity = new TenantEntity();
                        entity.setId(groupId);
                        groups.add(entity);
                    });
                    cloneAccessPolicy.setUserGroups(groups);

                    // create the access policy for the cloned policy
                    accessPolicyDAO.createAccessPolicy(cloneAccessPolicy);
                }
            }
        }
    }

    /**
     * Attempts to roll back and in the specified snippet.
     *
     * @param snippet snippet
     */
    public void rollbackClonedPolicies(final FlowSnippetDTO snippet) {
        snippet.getControllerServices().forEach(controllerServiceDTO -> {
            rollbackClonedPolicy(ResourceFactory.getComponentResource(ResourceType.ControllerService, controllerServiceDTO.getId(), controllerServiceDTO.getName()));
        });
        snippet.getFunnels().forEach(funnelDTO -> {
            rollbackClonedPolicy(ResourceFactory.getComponentResource(ResourceType.Funnel, funnelDTO.getId(), funnelDTO.getId()));
        });
        snippet.getInputPorts().forEach(inputPortDTO -> {
            rollbackClonedPolicy(ResourceFactory.getComponentResource(ResourceType.InputPort, inputPortDTO.getId(), inputPortDTO.getName()));
        });
        snippet.getLabels().forEach(labelDTO -> {
            rollbackClonedPolicy(ResourceFactory.getComponentResource(ResourceType.Label, labelDTO.getId(), labelDTO.getLabel()));
        });
        snippet.getOutputPorts().forEach(outputPortDTO -> {
            rollbackClonedPolicy(ResourceFactory.getComponentResource(ResourceType.OutputPort, outputPortDTO.getId(), outputPortDTO.getName()));
        });
        snippet.getProcessors().forEach(processorDTO -> {
            rollbackClonedPolicy(ResourceFactory.getComponentResource(ResourceType.Processor, processorDTO.getId(), processorDTO.getName()));
        });
        snippet.getRemoteProcessGroups().forEach(remoteProcessGroupDTO -> {
            rollbackClonedPolicy(ResourceFactory.getComponentResource(ResourceType.RemoteProcessGroup, remoteProcessGroupDTO.getId(), remoteProcessGroupDTO.getName()));
        });
        snippet.getProcessGroups().forEach(processGroupDTO -> {
            rollbackClonedPolicy(ResourceFactory.getComponentResource(ResourceType.ProcessGroup, processGroupDTO.getId(), processGroupDTO.getName()));

            // consider all descendant components
            if (processGroupDTO.getContents() != null) {
                rollbackClonedPolicies(processGroupDTO.getContents());
            }
        });
    }

    /**
     * Attempts to roll back all policies for the specified component. This includes the component resource, data resource
     * for the component, data transfer resource for the component, and policy resource for the component.
     *
     * @param componentResource component resource
     */
    private void rollbackClonedPolicy(final Resource componentResource) {
        final List<Resource> resources = new ArrayList<>();
        resources.add(componentResource);
        resources.add(ResourceFactory.getDataResource(componentResource));
        resources.add(ResourceFactory.getDataTransferResource(componentResource));
        resources.add(ResourceFactory.getPolicyResource(componentResource));

        for (final Resource resource : resources) {
            for (final RequestAction action : RequestAction.values()) {
                final AccessPolicy accessPolicy = accessPolicyDAO.getAccessPolicy(action, resource.getIdentifier());
                if (accessPolicy != null) {
                    try {
                        accessPolicyDAO.deleteAccessPolicy(accessPolicy.getIdentifier());
                    } catch (final Exception e) {
                        logger.warn(String.format("Unable to clean up cloned access policy for %s %s after failed copy/paste action.", action, componentResource.getIdentifier()), e);
                    }
                }
            }
        }
    }

    private void updateControllerServiceIdentifiers(final FlowSnippetDTO snippet, final Map<String, String> serviceIdMap) {
        final Set<ProcessorDTO> processors = snippet.getProcessors();
        if (processors != null) {
            for (final ProcessorDTO processor : processors) {
                updateControllerServiceIdentifiers(processor.getConfig(), serviceIdMap);
            }
        }

        for (final ProcessGroupDTO processGroupDto : snippet.getProcessGroups()) {
            updateControllerServiceIdentifiers(processGroupDto.getContents(), serviceIdMap);
        }
    }

    private void updateControllerServiceIdentifiers(final ProcessorConfigDTO configDto, final Map<String, String> serviceIdMap) {
        if (configDto == null) {
            return;
        }

        final Map<String, String> properties = configDto.getProperties();
        final Map<String, PropertyDescriptorDTO> descriptors = configDto.getDescriptors();
        if (properties != null && descriptors != null) {
            for (final PropertyDescriptorDTO descriptor : descriptors.values()) {
                if (descriptor.getIdentifiesControllerService() != null) {
                    final String currentServiceId = properties.get(descriptor.getName());
                    if (currentServiceId == null) {
                        continue;
                    }

                    // if this is a copy/paste action, we can continue to reference the same service, in this case
                    // the serviceIdMap will be empty
                    if (serviceIdMap.containsKey(currentServiceId)) {
                        final String newServiceId = serviceIdMap.get(currentServiceId);
                        properties.put(descriptor.getName(), newServiceId);
                    }
                }
            }
        }
    }

    /**
     * Generates a new id for the current id that is specified. If no seed is found, a new random id will be created.
     */
    private String generateId(final String currentId, final String seed, boolean isCopy) {
        long msb = UUID.fromString(currentId).getMostSignificantBits();
        int lsb = StringUtils.isBlank(seed)
                ? Math.abs(new Random().nextInt())
                : Math.abs(seed.hashCode());

        return isCopy ? TypeOneUUIDGenerator.generateId(msb, lsb).toString() : new UUID(msb, lsb).toString();
    }

    /* setters */
    public void setDtoFactory(final DtoFactory dtoFactory) {
        this.dtoFactory = dtoFactory;
    }

    public void setFlowController(final FlowController flowController) {
        this.flowController = flowController;
    }

    public void setAccessPolicyDAO(AccessPolicyDAO accessPolicyDAO) {
        this.accessPolicyDAO = accessPolicyDAO;
    }

    /**
     * Will normalize the coordinates of the processors to ensure their
     * consistency across exports. It will do so by fist calculating the
     * smallest X and smallest Y and then subtracting it from all X's and Y's of
     * each processor ensuring that coordinates are consistent across export
     * while preserving relative locations set by the user.
     */
    private void normalizeCoordinates(Collection<ProcessorDTO> processors) {
        double smallestX = Double.MAX_VALUE;
        double smallestY = Double.MAX_VALUE;
        for (ProcessorDTO processor : processors) {
            double d = processor.getPosition().getX();
            if (d < smallestX) {
                smallestX = d;
            }
            d = processor.getPosition().getY();
            if (d < smallestY) {
                smallestY = d;
            }
        }
        for (ProcessorDTO processor : processors) {
            processor.getPosition().setX(processor.getPosition().getX() - smallestX);
            processor.getPosition().setY(processor.getPosition().getY() - smallestY);
        }
    }

}

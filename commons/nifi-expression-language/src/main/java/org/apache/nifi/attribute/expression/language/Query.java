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
package org.apache.nifi.attribute.expression.language;

import static org.apache.nifi.attribute.expression.language.antlr.AttributeExpressionParser.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.nifi.attribute.expression.language.antlr.AttributeExpressionLexer;
import org.apache.nifi.attribute.expression.language.antlr.AttributeExpressionParser;
import org.apache.nifi.attribute.expression.language.evaluation.BooleanEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.DateEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.NumberEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.StringEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.cast.BooleanCastEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.cast.DateCastEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.cast.NumberCastEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.cast.StringCastEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.AndEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.AppendEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.AttributeEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.ContainsEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.DateToNumberEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.DivideEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.EndsWithEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.EqualsEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.EqualsIgnoreCaseEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.FindEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.FormatEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.GreaterThanEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.GreaterThanOrEqualEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.HostnameEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.IPEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.IndexOfEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.IsEmptyEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.IsNullEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.LastIndexOfEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.LengthEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.LessThanEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.LessThanOrEqualEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.MatchesEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.MinusEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.ModEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.MultiplyEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.NotEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.NotNullEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.NowEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.NumberToDateEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.OneUpSequenceEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.OrEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.PlusEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.PrependEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.ReplaceAllEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.ReplaceEmptyEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.ReplaceEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.ReplaceNullEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.StartsWithEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.StringToDateEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.SubstringAfterEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.SubstringAfterLastEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.SubstringBeforeEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.SubstringBeforeLastEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.SubstringEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.ToLowerEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.ToNumberEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.ToRadixEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.ToStringEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.ToUpperEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.TrimEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.UrlDecodeEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.UrlEncodeEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.functions.UuidEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.literals.BooleanLiteralEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.literals.NumberLiteralEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.literals.StringLiteralEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.reduce.CountEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.reduce.JoinEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.reduce.ReduceEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.selection.AllAttributesEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.selection.AnyAttributeEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.selection.DelineatedAttributeEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.selection.MultiAttributeEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.selection.MultiMatchAttributeEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.selection.MultiNamedAttributeEvaluator;
import org.apache.nifi.attribute.expression.language.exception.AttributeExpressionLanguageException;
import org.apache.nifi.attribute.expression.language.exception.AttributeExpressionLanguageParsingException;
import org.apache.nifi.expression.AttributeExpression.ResultType;
import org.apache.nifi.expression.AttributeValueDecorator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.exception.ProcessException;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.Tree;

/**
 * Class used for creating and evaluating NiFi Expression Language. Once a Query
 * has been created, it may be evaluated using the evaluate methods exactly
 * once.
 */
public class Query {

    private final String query;
    private final Tree tree;
    private final Evaluator<?> evaluator;
    private final AtomicBoolean evaluated = new AtomicBoolean(false);

    private Query(final String query, final Tree tree, final Evaluator<?> evaluator) {
        this.query = query;
        this.tree = tree;
        this.evaluator = evaluator;
    }

    public static boolean isValidExpression(final String value) {
        try {
            validateExpression(value, false);
            return true;
        } catch (final ProcessException e) {
            return false;
        }
    }

    public static ResultType getResultType(final String value) throws AttributeExpressionLanguageParsingException {
        return Query.compile(value).getResultType();
    }

    public static List<ResultType> extractResultTypes(final String value) throws AttributeExpressionLanguageParsingException {
        final List<ResultType> types = new ArrayList<>();

        for (final Range range : extractExpressionRanges(value)) {
            final String text = value.substring(range.getStart(), range.getEnd() + 1);
            types.add(getResultType(text));
        }

        return types;
    }

    public static List<String> extractExpressions(final String value) throws AttributeExpressionLanguageParsingException {
        final List<String> expressions = new ArrayList<>();

        for (final Range range : extractExpressionRanges(value)) {
            expressions.add(value.substring(range.getStart(), range.getEnd() + 1));
        }

        return expressions;
    }

    public static List<Range> extractExpressionRanges(final String value) throws AttributeExpressionLanguageParsingException {
        final List<Range> ranges = new ArrayList<>();
        char lastChar = 0;
        int embeddedCount = 0;
        int expressionStart = -1;
        boolean oddDollarCount = false;
        int backslashCount = 0;

        charLoop:
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);

            if (expressionStart > -1 && (c == '\'' || c == '"') && (lastChar != '\\' || backslashCount % 2 == 0)) {
                final int endQuoteIndex = findEndQuoteChar(value, i);
                if (endQuoteIndex < 0) {
                    break charLoop;
                }

                i = endQuoteIndex;
                continue;
            }

            if (c == '{') {
                if (oddDollarCount && lastChar == '$') {
                    if (embeddedCount == 0) {
                        expressionStart = i - 1;
                    }
                }

                embeddedCount++;
            } else if (c == '}') {
                if (embeddedCount <= 0) {
                    continue;
                }

                if (--embeddedCount == 0) {
                    if (expressionStart > -1) {
                        // ended expression. Add a new range.
                        final Range range = new Range(expressionStart, i);
                        ranges.add(range);
                    }

                    expressionStart = -1;
                }
            } else if (c == '$') {
                oddDollarCount = !oddDollarCount;
            } else if (c == '\\') {
                backslashCount++;
            } else {
                oddDollarCount = false;
            }

            lastChar = c;
        }

        return ranges;
    }

    /**
     *
     *
     * @param value
     * @param allowSurroundingCharacters
     * @throws AttributeExpressionLanguageParsingException
     */
    public static void validateExpression(final String value, final boolean allowSurroundingCharacters) throws AttributeExpressionLanguageParsingException {
        if (!allowSurroundingCharacters) {
            final List<Range> ranges = extractExpressionRanges(value);
            if (ranges.size() > 1) {
                throw new AttributeExpressionLanguageParsingException("Found multiple Expressions but expected only 1");
            }

            if (ranges.isEmpty()) {
                throw new AttributeExpressionLanguageParsingException("No Expressions found");
            }

            final Range range = ranges.get(0);
            final String expression = value.substring(range.getStart(), range.getEnd() + 1);
            Query.compile(expression);

            if (range.getStart() > 0 || range.getEnd() < value.length() - 1) {
                throw new AttributeExpressionLanguageParsingException("Found characters outside of Expression");
            }
        } else {
            for (final Range range : extractExpressionRanges(value)) {
                final String expression = value.substring(range.getStart(), range.getEnd() + 1);
                Query.compile(expression);
            }
        }
    }

    static int findEndQuoteChar(final String value, final int quoteStart) {
        final char quoteChar = value.charAt(quoteStart);

        int backslashCount = 0;
        char lastChar = 0;
        for (int i = quoteStart + 1; i < value.length(); i++) {
            final char c = value.charAt(i);

            if (c == '\\') {
                backslashCount++;
            } else if (c == quoteChar && ((backslashCount % 2 == 0) || lastChar != '\\')) {
                return i;
            }

            lastChar = c;
        }

        return -1;
    }

    static String evaluateExpression(final Tree tree, final String queryText, final Map<String, String> expressionMap, final AttributeValueDecorator decorator) throws ProcessException {
        final Object evaluated = Query.fromTree(tree, queryText).evaluate(expressionMap).getValue();
        if (evaluated == null) {
            return null;
        }

        final String value = evaluated.toString();
        final String escaped = value.replace("$$", "$");
        return (decorator == null) ? escaped : decorator.decorate(escaped);
    }

    static String evaluateExpressions(final String rawValue, Map<String, String> expressionMap) throws ProcessException {
        return evaluateExpressions(rawValue, expressionMap, null);
    }

    static String evaluateExpressions(final String rawValue) throws ProcessException {
        return evaluateExpressions(rawValue, createExpressionMap(null), null);
    }

    static String evaluateExpressions(final String rawValue, final FlowFile flowFile) throws ProcessException {
        return evaluateExpressions(rawValue, createExpressionMap(flowFile), null);
    }

    static String evaluateExpressions(final String rawValue, Map<String, String> expressionMap, final AttributeValueDecorator decorator) throws ProcessException {
        return Query.prepare(rawValue).evaluateExpressions(expressionMap, decorator);
    }

    public static String evaluateExpressions(final String rawValue, final FlowFile flowFile, final AttributeValueDecorator decorator) throws ProcessException {
        if (rawValue == null) {
            return null;
        }

        final Map<String, String> expressionMap = createExpressionMap(flowFile);
        return evaluateExpressions(rawValue, expressionMap, decorator);
    }

    private static Evaluator<?> getRootSubjectEvaluator(final Evaluator<?> evaluator) {
        if (evaluator == null) {
            return null;
        }

        final Evaluator<?> subject = evaluator.getSubjectEvaluator();
        if (subject == null) {
            return evaluator;
        }

        return getRootSubjectEvaluator(subject);
    }

    /**
     * Un-escapes ${...} patterns that were escaped
     *
     * @param value
     * @return
     */
    public static String unescape(final String value) {
        return value.replaceAll("\\$\\$(?=\\$*\\{.*?\\})", "\\$");
    }

    static Map<String, String> createExpressionMap(final FlowFile flowFile) {
        final Map<String, String> attributeMap = flowFile == null ? new HashMap<String, String>() : flowFile.getAttributes();
        final Map<String, String> envMap = System.getenv();
        final Map<?, ?> sysProps = System.getProperties();

        final Map<String, String> flowFileProps = new HashMap<>();
        if (flowFile != null) {
            flowFileProps.put("flowFileId", String.valueOf(flowFile.getId()));
            flowFileProps.put("fileSize", String.valueOf(flowFile.getSize()));
            flowFileProps.put("entryDate", String.valueOf(flowFile.getEntryDate()));
            flowFileProps.put("lineageStartDate", String.valueOf(flowFile.getLineageStartDate()));
        }

        return wrap(attributeMap, flowFileProps, envMap, sysProps);
    }

    private static Map<String, String> wrap(final Map<String, String> attributes, final Map<String, String> flowFileProps,
            final Map<String, String> env, final Map<?, ?> sysProps) {
        @SuppressWarnings("rawtypes")
        final Map[] maps = new Map[]{attributes, flowFileProps, env, sysProps};

        return new Map<String, String>() {
            @Override
            public int size() {
                int size = 0;
                for (final Map<?, ?> map : maps) {
                    size += map.size();
                }
                return size;
            }

            @Override
            public boolean isEmpty() {
                for (final Map<?, ?> map : maps) {
                    if (!map.isEmpty()) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean containsKey(final Object key) {
                if (key == null) {
                    return false;
                }
                if (!(key instanceof String)) {
                    return false;
                }

                for (final Map<?, ?> map : maps) {
                    if (map.containsKey(key)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean containsValue(final Object value) {
                for (final Map<?, ?> map : maps) {
                    if (map.containsValue(value)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            @SuppressWarnings("rawtypes")
            public String get(final Object key) {
                if (key == null) {
                    throw new IllegalArgumentException("Null Keys are not allowed");
                }
                if (!(key instanceof String)) {
                    return null;
                }

                for (final Map map : maps) {
                    final Object val = map.get(key);
                    if (val != null) {
                        return String.valueOf(val);
                    }
                }
                return null;
            }

            @Override
            public String put(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String remove(final Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(final Map<? extends String, ? extends String> m) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Set<String> keySet() {
                final Set<String> keySet = new HashSet<>();
                for (final Map map : maps) {
                    keySet.addAll(map.keySet());
                }
                return keySet;
            }

            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Collection<String> values() {
                final Set<String> values = new HashSet<>();
                for (final Map map : maps) {
                    values.addAll(map.values());
                }
                return values;
            }

            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Set<java.util.Map.Entry<String, String>> entrySet() {
                final Set<java.util.Map.Entry<String, String>> entrySet = new HashSet<>();
                for (final Map map : maps) {
                    entrySet.addAll(map.entrySet());
                }
                return entrySet;
            }

        };
    }

    public static Query fromTree(final Tree tree, final String text) {
        return new Query(text, tree, buildEvaluator(tree));
    }

    public static Tree compileTree(final String query) throws AttributeExpressionLanguageParsingException {
        try {
            final CommonTokenStream lexerTokenStream = createTokenStream(query);
            final AttributeExpressionParser parser = new AttributeExpressionParser(lexerTokenStream);
            final Tree ast = (Tree) parser.query().getTree();
            final Tree tree = ast.getChild(0);

            // ensure that we are able to build the evaluators, so that we validate syntax
            final Evaluator<?> evaluator = buildEvaluator(tree);
            verifyMappingEvaluatorReduced(evaluator);
            return tree;
        } catch (final AttributeExpressionLanguageParsingException e) {
            throw e;
        } catch (final Exception e) {
            throw new AttributeExpressionLanguageParsingException(e);
        }
    }

    public static PreparedQuery prepare(final String query) throws AttributeExpressionLanguageParsingException {
        if (query == null) {
            return new EmptyPreparedQuery(null);
        }

        final List<Range> ranges = extractExpressionRanges(query);

        if (ranges.isEmpty()) {
            return new EmptyPreparedQuery(query.replace("$$", "$"));
        }

        try {
            final List<String> substrings = new ArrayList<>();
            final Map<String, Tree> trees = new HashMap<>();
    
            int lastIndex = 0;
            for (final Range range : ranges) {
                if (range.getStart() > lastIndex) {
                    substrings.add(query.substring(lastIndex, range.getStart()).replace("$$", "$"));
                    lastIndex = range.getEnd() + 1;
                }
    
                final String treeText = query.substring(range.getStart(), range.getEnd() + 1).replace("$$", "$");
                substrings.add(treeText);
                trees.put(treeText, Query.compileTree(treeText));
                lastIndex = range.getEnd() + 1;
            }
    
            final Range lastRange = ranges.get(ranges.size() - 1);
            if (lastRange.getEnd() + 1 < query.length()) {
                final String treeText = query.substring(lastRange.getEnd() + 1).replace("$$", "$");
                substrings.add(treeText);
            }
    
            return new StandardPreparedQuery(substrings, trees);
        } catch (final AttributeExpressionLanguageParsingException e) {
            return new InvalidPreparedQuery(query, e.getMessage());
        }
    }

    public static Query compile(final String query) throws AttributeExpressionLanguageParsingException {
        try {
            final CommonTokenStream lexerTokenStream = createTokenStream(query);
            final AttributeExpressionParser parser = new AttributeExpressionParser(lexerTokenStream);
            final Tree ast = (Tree) parser.query().getTree();
            final Tree tree = ast.getChild(0);

            final Evaluator<?> evaluator = buildEvaluator(tree);
            verifyMappingEvaluatorReduced(evaluator);
            
            return new Query(query, tree, evaluator);
        } catch (final AttributeExpressionLanguageParsingException e) {
            throw e;
        } catch (final Exception e) {
            throw new AttributeExpressionLanguageParsingException(e);
        }
    }

    
    private static void verifyMappingEvaluatorReduced(final Evaluator<?> evaluator) {
        // if the result type of the evaluator is BOOLEAN, then it will always
        // be reduced when evaluator.
        final ResultType resultType = evaluator.getResultType();
        if (resultType == ResultType.BOOLEAN) {
            return;
        }

        final Evaluator<?> rootEvaluator = getRootSubjectEvaluator(evaluator);
        if (rootEvaluator != null && rootEvaluator instanceof MultiAttributeEvaluator) {
            final MultiAttributeEvaluator multiAttrEval = (MultiAttributeEvaluator) rootEvaluator;
            switch (multiAttrEval.getEvaluationType()) {
            case ALL_ATTRIBUTES:
            case ALL_MATCHING_ATTRIBUTES:
            case ALL_DELINEATED_VALUES: {
                if (!(evaluator instanceof ReduceEvaluator)) {
                    throw new AttributeExpressionLanguageParsingException("Cannot evaluate expression because it attempts to reference multiple attributes but does not use a reducing function");
                }
                break;
            }
            default:
                throw new AttributeExpressionLanguageParsingException("Cannot evaluate expression because it attempts to reference multiple attributes but does not use a reducing function");
            }
        }
    }
    
    private static CommonTokenStream createTokenStream(final String expression) throws AttributeExpressionLanguageParsingException {
        final CharStream input = new ANTLRStringStream(expression);
        final AttributeExpressionLexer lexer = new AttributeExpressionLexer(input);
        return new CommonTokenStream(lexer);
    }

    public ResultType getResultType() {
        return evaluator.getResultType();
    }

    QueryResult<?> evaluate() {
        return evaluate(createExpressionMap(null));
    }

    QueryResult<?> evaluate(final FlowFile flowFile) {
        return evaluate(createExpressionMap(flowFile));
    }

    QueryResult<?> evaluate(final Map<String, String> attributes) {
        if (evaluated.getAndSet(true)) {
            throw new IllegalStateException("A Query cannot be evaluated more than once");
        }

        return evaluator.evaluate(attributes);
    }

    Tree getTree() {
        return this.tree;
    }

    @Override
    public String toString() {
        return "Query [" + query + "]";
    }

    private static StringEvaluator newStringLiteralEvaluator(final String literalValue) {
        if (literalValue == null || literalValue.length() < 2) {
            return new StringLiteralEvaluator(literalValue);
        }

        final List<Range> ranges = extractExpressionRanges(literalValue);
        if (ranges.isEmpty()) {
            return new StringLiteralEvaluator(literalValue);
        }

        final List<Evaluator<?>> evaluators = new ArrayList<>();

        int lastIndex = 0;
        for (final Range range : ranges) {
            if (range.getStart() > lastIndex) {
                evaluators.add(newStringLiteralEvaluator(literalValue.substring(lastIndex, range.getStart())));
            }

            final String treeText = literalValue.substring(range.getStart(), range.getEnd() + 1);
            evaluators.add(buildEvaluator(compileTree(treeText)));
            lastIndex = range.getEnd() + 1;
        }

        final Range lastRange = ranges.get(ranges.size() - 1);
        if (lastRange.getEnd() + 1 < literalValue.length()) {
            final String treeText = literalValue.substring(lastRange.getEnd() + 1);
            evaluators.add(newStringLiteralEvaluator(treeText));
        }

        if (evaluators.size() == 1) {
            return toStringEvaluator(evaluators.get(0));
        }

        StringEvaluator lastEvaluator = toStringEvaluator(evaluators.get(0));
        for (int i = 1; i < evaluators.size(); i++) {
            lastEvaluator = new AppendEvaluator(lastEvaluator, toStringEvaluator(evaluators.get(i)));
        }

        return lastEvaluator;
    }

    private static Evaluator<?> buildEvaluator(final Tree tree) {
        switch (tree.getType()) {
            case EXPRESSION: {
                return buildExpressionEvaluator(tree);
            }
            case ATTRIBUTE_REFERENCE: {
                final Evaluator<?> childEvaluator = buildEvaluator(tree.getChild(0));
                if (childEvaluator instanceof MultiAttributeEvaluator) {
                    return childEvaluator;
                }
                return new AttributeEvaluator(toStringEvaluator(childEvaluator));
            }
            case MULTI_ATTRIBUTE_REFERENCE: {

                final Tree functionTypeTree = tree.getChild(0);
                final int multiAttrType = functionTypeTree.getType();
                if (multiAttrType == ANY_DELINEATED_VALUE || multiAttrType == ALL_DELINEATED_VALUES) {
                    final StringEvaluator delineatedValueEvaluator = toStringEvaluator(buildEvaluator(tree.getChild(1)));
                    final StringEvaluator delimiterEvaluator = toStringEvaluator(buildEvaluator(tree.getChild(2)));

                    return new DelineatedAttributeEvaluator(delineatedValueEvaluator, delimiterEvaluator, multiAttrType);
                }

                final List<String> attributeNames = new ArrayList<>();
                for (int i = 1; i < tree.getChildCount(); i++) {  // skip the first child because that's the name of the multi-attribute function
                    attributeNames.add(newStringLiteralEvaluator(tree.getChild(i).getText()).evaluate(null).getValue());
                }

                switch (multiAttrType) {
                    case ALL_ATTRIBUTES:
                        for (final String attributeName : attributeNames) {
                            try {
                                FlowFile.KeyValidator.validateKey(attributeName);
                            } catch (final IllegalArgumentException iae) {
                                throw new AttributeExpressionLanguageParsingException("Invalid Attribute Name: " + attributeName + ". " + iae.getMessage());
                            }
                        }

                        return new MultiNamedAttributeEvaluator(attributeNames, ALL_ATTRIBUTES);
                    case ALL_MATCHING_ATTRIBUTES:
                        return new MultiMatchAttributeEvaluator(attributeNames, ALL_MATCHING_ATTRIBUTES);
                    case ANY_ATTRIBUTE:
                        for (final String attributeName : attributeNames) {
                            try {
                                FlowFile.KeyValidator.validateKey(attributeName);
                            } catch (final IllegalArgumentException iae) {
                                throw new AttributeExpressionLanguageParsingException("Invalid Attribute Name: " + attributeName + ". " + iae.getMessage());
                            }
                        }

                        return new MultiNamedAttributeEvaluator(attributeNames, ANY_ATTRIBUTE);
                    case ANY_MATCHING_ATTRIBUTE:
                        return new MultiMatchAttributeEvaluator(attributeNames, ANY_MATCHING_ATTRIBUTE);
                    default:
                        throw new AssertionError("Illegal Multi-Attribute Reference: " + functionTypeTree.toString());
                }
            }
            case ATTR_NAME: {
                return newStringLiteralEvaluator(tree.getChild(0).getText());
            }
            case NUMBER: {
                return new NumberLiteralEvaluator(tree.getText());
            }
            case STRING_LITERAL: {
                return newStringLiteralEvaluator(tree.getText());
            }
            case TRUE:
            case FALSE:
                return buildBooleanEvaluator(tree);
            case UUID: {
                return new UuidEvaluator();
            }
            case NOW: {
                return new NowEvaluator();
            }
            case IP: {
                try {
                    return new IPEvaluator();
                } catch (final UnknownHostException e) {
                    throw new AttributeExpressionLanguageException(e);
                }
            }
            case HOSTNAME: {
                if (tree.getChildCount() == 0) {
                    try {
                        return new HostnameEvaluator(false);
                    } catch (UnknownHostException e) {
                        throw new AttributeExpressionLanguageException(e);
                    }
                } else if (tree.getChildCount() == 1) {
                    final Tree childTree = tree.getChild(0);
                    try {
                        switch (childTree.getType()) {
                            case TRUE:
                                return new HostnameEvaluator(true);
                            case FALSE:
                                return new HostnameEvaluator(false);
                            default:
                                throw new AttributeExpressionLanguageParsingException("Call to hostname() must take 0 or 1 (boolean) parameter");
                        }
                    } catch (UnknownHostException e) {
                        throw new AttributeExpressionLanguageException(e);
                    }
                } else {
                    throw new AttributeExpressionLanguageParsingException("Call to hostname() must take 0 or 1 (boolean) parameter");
                }
            }
            case NEXT_INT: {
                return new OneUpSequenceEvaluator();
            }
            default:
                throw new AttributeExpressionLanguageParsingException("Unexpected token: " + tree.toString());
        }
    }

    private static Evaluator<Boolean> buildBooleanEvaluator(final Tree tree) {
        switch (tree.getType()) {
            case TRUE:
                return new BooleanLiteralEvaluator(true);
            case FALSE:
                return new BooleanLiteralEvaluator(false);
        }
        throw new AttributeExpressionLanguageParsingException("Cannot build Boolean evaluator from tree " + tree.toString());
    }

    private static Evaluator<?> buildExpressionEvaluator(final Tree tree) {
        if (tree.getChildCount() == 0) {
            throw new AttributeExpressionLanguageParsingException("EXPRESSION tree node has no children");
        }
        
        final Evaluator<?> evaluator;
        if (tree.getChildCount() == 1) {
            evaluator = buildEvaluator(tree.getChild(0));
        } else {
            // we can chain together functions in the form of:
            // ${x:trim():substring(1,2):trim()}
            // in this case, the subject of the right-most function is the function to its left; its
            // subject is the function to its left (the first trim()), and its subject is the value of
            // the 'x' attribute. We accomplish this logic by iterating over all of the children of the
            // tree from the right-most child going left-ward.
            evaluator = buildFunctionExpressionEvaluator(tree, 0);
        }
        
        Evaluator<?> chosenEvaluator = evaluator;
        final Evaluator<?> rootEvaluator = getRootSubjectEvaluator(evaluator);
        if (rootEvaluator != null) {
            if (rootEvaluator instanceof MultiAttributeEvaluator) {
                final MultiAttributeEvaluator multiAttrEval = (MultiAttributeEvaluator) rootEvaluator;

                switch (multiAttrEval.getEvaluationType()) {
                    case ANY_ATTRIBUTE:
                    case ANY_MATCHING_ATTRIBUTE:
                    case ANY_DELINEATED_VALUE:
                        chosenEvaluator = new AnyAttributeEvaluator((BooleanEvaluator) evaluator, multiAttrEval);
                        break;
                    case ALL_ATTRIBUTES:
                    case ALL_MATCHING_ATTRIBUTES:
                    case ALL_DELINEATED_VALUES:
                        chosenEvaluator = new AllAttributesEvaluator((BooleanEvaluator) evaluator, multiAttrEval);
                        break;
                }
            }
        }
        
        return chosenEvaluator;
    }

    private static Evaluator<?> buildFunctionExpressionEvaluator(final Tree tree, final int offset) {
        if (tree.getChildCount() == 0) {
            throw new AttributeExpressionLanguageParsingException("EXPRESSION tree node has no children");
        }
        final int firstChildIndex = tree.getChildCount() - offset - 1;
        if (firstChildIndex == 0) {
            return buildEvaluator(tree.getChild(0));
        }

        final Tree functionTree = tree.getChild(firstChildIndex);
        final Evaluator<?> subjectEvaluator = buildFunctionExpressionEvaluator(tree, offset + 1);

        final Tree functionNameTree = functionTree.getChild(0);
        final List<Evaluator<?>> argEvaluators = new ArrayList<>();
        for (int i = 1; i < functionTree.getChildCount(); i++) {
            argEvaluators.add(buildEvaluator(functionTree.getChild(i)));
        }
        return buildFunctionEvaluator(functionNameTree, subjectEvaluator, argEvaluators);
    }

    private static List<Evaluator<?>> verifyArgCount(final List<Evaluator<?>> args, final int count, final String functionName) {
        if (args.size() != count) {
            throw new AttributeExpressionLanguageParsingException(functionName + "() function takes " + count + " arguments");
        }
        return args;
    }

    private static StringEvaluator toStringEvaluator(final Evaluator<?> evaluator) {
        return toStringEvaluator(evaluator, null);
    }

    private static StringEvaluator toStringEvaluator(final Evaluator<?> evaluator, final String location) {
        if (evaluator.getResultType() == ResultType.STRING) {
            return (StringEvaluator) evaluator;
        }

        return new StringCastEvaluator(evaluator);
    }

    private static BooleanEvaluator toBooleanEvaluator(final Evaluator<?> evaluator, final String location) {
        switch (evaluator.getResultType()) {
            case BOOLEAN:
                return (BooleanEvaluator) evaluator;
            case STRING:
                return new BooleanCastEvaluator((StringEvaluator) evaluator);
            default:
                throw new AttributeExpressionLanguageParsingException("Cannot implicitly convert Data Type " + evaluator.getResultType() + " to " + ResultType.BOOLEAN
                        + (location == null ? "" : " at location [" + location + "]"));
        }

    }

    private static BooleanEvaluator toBooleanEvaluator(final Evaluator<?> evaluator) {
        return toBooleanEvaluator(evaluator, null);
    }

    private static NumberEvaluator toNumberEvaluator(final Evaluator<?> evaluator) {
        return toNumberEvaluator(evaluator, null);
    }

    private static NumberEvaluator toNumberEvaluator(final Evaluator<?> evaluator, final String location) {
        switch (evaluator.getResultType()) {
            case NUMBER:
                return (NumberEvaluator) evaluator;
            case STRING:
                return new NumberCastEvaluator((StringEvaluator) evaluator);
            case DATE:
                return new DateToNumberEvaluator((DateEvaluator) evaluator);
            default:
                throw new AttributeExpressionLanguageParsingException("Cannot implicitly convert Data Type " + evaluator.getResultType() + " to " + ResultType.NUMBER
                        + (location == null ? "" : " at location [" + location + "]"));
        }
    }

    private static DateEvaluator toDateEvaluator(final Evaluator<?> evaluator) {
        return toDateEvaluator(evaluator, null);
    }

    private static DateEvaluator toDateEvaluator(final Evaluator<?> evaluator, final String location) {
        if (evaluator.getResultType() == ResultType.DATE) {
            return (DateEvaluator) evaluator;
        }

        return new DateCastEvaluator(evaluator);
    }

    private static Evaluator<?> buildFunctionEvaluator(final Tree tree, final Evaluator<?> subjectEvaluator, final List<Evaluator<?>> argEvaluators) {
        switch (tree.getType()) {
            case TRIM: {
                verifyArgCount(argEvaluators, 0, "trim");
                return new TrimEvaluator(toStringEvaluator(subjectEvaluator));
            }
            case TO_STRING: {
                verifyArgCount(argEvaluators, 0, "toString");
                return new ToStringEvaluator(subjectEvaluator);
            }
            case TO_LOWER: {
                verifyArgCount(argEvaluators, 0, "toLower");
                return new ToLowerEvaluator(toStringEvaluator(subjectEvaluator));
            }
            case TO_UPPER: {
                verifyArgCount(argEvaluators, 0, "toUpper");
                return new ToUpperEvaluator(toStringEvaluator(subjectEvaluator));
            }
            case URL_ENCODE: {
                verifyArgCount(argEvaluators, 0, "urlEncode");
                return new UrlEncodeEvaluator(toStringEvaluator(subjectEvaluator));
            }
            case URL_DECODE: {
                verifyArgCount(argEvaluators, 0, "urlDecode");
                return new UrlDecodeEvaluator(toStringEvaluator(subjectEvaluator));
            }
            case SUBSTRING_BEFORE: {
                verifyArgCount(argEvaluators, 1, "substringBefore");
                return new SubstringBeforeEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to substringBefore"));
            }
            case SUBSTRING_BEFORE_LAST: {
                verifyArgCount(argEvaluators, 1, "substringBeforeLast");
                return new SubstringBeforeLastEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to substringBeforeLast"));
            }
            case SUBSTRING_AFTER: {
                verifyArgCount(argEvaluators, 1, "substringAfter");
                return new SubstringAfterEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to substringAfter"));
            }
            case SUBSTRING_AFTER_LAST: {
                verifyArgCount(argEvaluators, 1, "substringAfterLast");
                return new SubstringAfterLastEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to substringAfterLast"));
            }
            case REPLACE_NULL: {
                verifyArgCount(argEvaluators, 1, "replaceNull");
                return new ReplaceNullEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to replaceNull"));
            }
            case REPLACE_EMPTY: {
                verifyArgCount(argEvaluators, 1, "replaceEmtpy");
                return new ReplaceEmptyEvaluator(toStringEvaluator(subjectEvaluator), toStringEvaluator(argEvaluators.get(0), "first argumen to replaceEmpty"));
            }
            case REPLACE: {
                verifyArgCount(argEvaluators, 2, "replace");
                return new ReplaceEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to replace"),
                        toStringEvaluator(argEvaluators.get(1), "second argument to replace"));
            }
            case REPLACE_ALL: {
                verifyArgCount(argEvaluators, 2, "replaceAll");
                return new ReplaceAllEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to replaceAll"),
                        toStringEvaluator(argEvaluators.get(1), "second argument to replaceAll"));
            }
            case APPEND: {
                verifyArgCount(argEvaluators, 1, "append");
                return new AppendEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to append"));
            }
            case PREPEND: {
                verifyArgCount(argEvaluators, 1, "prepend");
                return new PrependEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to prepend"));
            }
            case SUBSTRING: {
                final int numArgs = argEvaluators.size();
                if (numArgs == 1) {
                    return new SubstringEvaluator(toStringEvaluator(subjectEvaluator),
                            toNumberEvaluator(argEvaluators.get(0), "first argument to substring"));
                } else if (numArgs == 2) {
                    return new SubstringEvaluator(toStringEvaluator(subjectEvaluator),
                            toNumberEvaluator(argEvaluators.get(0), "first argument to substring"),
                            toNumberEvaluator(argEvaluators.get(1), "second argument to substring"));
                } else {
                    throw new AttributeExpressionLanguageParsingException("substring() function can take either 1 or 2 arguments but cannot take " + numArgs + " arguments");
                }
            }
            case JOIN: {
                verifyArgCount(argEvaluators, 1, "join");
                return new JoinEvaluator(toStringEvaluator(subjectEvaluator), toStringEvaluator(argEvaluators.get(0)));
            }
            case COUNT: {
                verifyArgCount(argEvaluators, 0, "count");
                return new CountEvaluator(subjectEvaluator);
            }
            case IS_NULL: {
                verifyArgCount(argEvaluators, 0, "isNull");
                return new IsNullEvaluator(toStringEvaluator(subjectEvaluator));
            }
            case IS_EMPTY: {
                verifyArgCount(argEvaluators, 0, "isNull");
                return new IsEmptyEvaluator(toStringEvaluator(subjectEvaluator));
            }
            case NOT_NULL: {
                verifyArgCount(argEvaluators, 0, "notNull");
                return new NotNullEvaluator(toStringEvaluator(subjectEvaluator));
            }
            case STARTS_WITH: {
                verifyArgCount(argEvaluators, 1, "startsWith");
                return new StartsWithEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to startsWith"));
            }
            case ENDS_WITH: {
                verifyArgCount(argEvaluators, 1, "endsWith");
                return new EndsWithEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to endsWith"));
            }
            case CONTAINS: {
                verifyArgCount(argEvaluators, 1, "contains");
                return new ContainsEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to contains"));
            }
            case FIND: {
                verifyArgCount(argEvaluators, 1, "find");
                return new FindEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to find"));
            }
            case MATCHES: {
                verifyArgCount(argEvaluators, 1, "matches");
                return new MatchesEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to matches"));
            }
            case EQUALS: {
                verifyArgCount(argEvaluators, 1, "equals");
                return new EqualsEvaluator(subjectEvaluator, argEvaluators.get(0));
            }
            case EQUALS_IGNORE_CASE: {
                verifyArgCount(argEvaluators, 1, "equalsIgnoreCase");
                return new EqualsIgnoreCaseEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to equalsIgnoreCase"));
            }
            case GREATER_THAN: {
                verifyArgCount(argEvaluators, 1, "gt");
                return new GreaterThanEvaluator(toNumberEvaluator(subjectEvaluator),
                        toNumberEvaluator(argEvaluators.get(0), "first argument to gt"));
            }
            case GREATER_THAN_OR_EQUAL: {
                verifyArgCount(argEvaluators, 1, "ge");
                return new GreaterThanOrEqualEvaluator(toNumberEvaluator(subjectEvaluator),
                        toNumberEvaluator(argEvaluators.get(0), "first argument to ge"));
            }
            case LESS_THAN: {
                verifyArgCount(argEvaluators, 1, "lt");
                return new LessThanEvaluator(toNumberEvaluator(subjectEvaluator),
                        toNumberEvaluator(argEvaluators.get(0), "first argument to lt"));
            }
            case LESS_THAN_OR_EQUAL: {
                verifyArgCount(argEvaluators, 1, "le");
                return new LessThanOrEqualEvaluator(toNumberEvaluator(subjectEvaluator),
                        toNumberEvaluator(argEvaluators.get(0), "first argument to le"));
            }
            case LENGTH: {
                verifyArgCount(argEvaluators, 0, "length");
                return new LengthEvaluator(toStringEvaluator(subjectEvaluator));
            }
            case TO_DATE: {
                if (argEvaluators.isEmpty()) {
                    return new NumberToDateEvaluator(toNumberEvaluator(subjectEvaluator));
                } else if (subjectEvaluator.getResultType() == ResultType.STRING) {
                    return new StringToDateEvaluator(toStringEvaluator(subjectEvaluator), toStringEvaluator(argEvaluators.get(0)));
                } else {
                    return new NumberToDateEvaluator(toNumberEvaluator(subjectEvaluator));
                }
            }
            case TO_NUMBER: {
                verifyArgCount(argEvaluators, 0, "toNumber");
                switch (subjectEvaluator.getResultType()) {
                    case STRING:
                        return new ToNumberEvaluator((StringEvaluator) subjectEvaluator);
                    case DATE:
                        return new DateToNumberEvaluator((DateEvaluator) subjectEvaluator);
                    default:
                        throw new AttributeExpressionLanguageParsingException(subjectEvaluator + " returns type " + subjectEvaluator.getResultType() + " but expected to get " + ResultType.STRING);
                }
            }
            case TO_RADIX: {
                if (argEvaluators.size() == 1) {
                    return new ToRadixEvaluator((NumberEvaluator) subjectEvaluator, toNumberEvaluator(argEvaluators.get(0)));
                } else {
                    return new ToRadixEvaluator((NumberEvaluator) subjectEvaluator, toNumberEvaluator(argEvaluators.get(0)), toNumberEvaluator(argEvaluators.get(1)));
                }
            }
            case MOD: {
                return new ModEvaluator(toNumberEvaluator(subjectEvaluator), toNumberEvaluator(argEvaluators.get(0)));
            }
            case PLUS: {
                return new PlusEvaluator(toNumberEvaluator(subjectEvaluator), toNumberEvaluator(argEvaluators.get(0)));
            }
            case MINUS: {
                return new MinusEvaluator(toNumberEvaluator(subjectEvaluator), toNumberEvaluator(argEvaluators.get(0)));
            }
            case MULTIPLY: {
                return new MultiplyEvaluator(toNumberEvaluator(subjectEvaluator), toNumberEvaluator(argEvaluators.get(0)));
            }
            case DIVIDE: {
                return new DivideEvaluator(toNumberEvaluator(subjectEvaluator), toNumberEvaluator(argEvaluators.get(0)));
            }
            case INDEX_OF: {
                verifyArgCount(argEvaluators, 1, "indexOf");
                return new IndexOfEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to indexOf"));
            }
            case LAST_INDEX_OF: {
                verifyArgCount(argEvaluators, 1, "lastIndexOf");
                return new LastIndexOfEvaluator(toStringEvaluator(subjectEvaluator),
                        toStringEvaluator(argEvaluators.get(0), "first argument to lastIndexOf"));
            }
            case FORMAT: {
                return new FormatEvaluator(toDateEvaluator(subjectEvaluator), toStringEvaluator(argEvaluators.get(0), "first argument of format"));
            }
            case OR: {
                return new OrEvaluator(toBooleanEvaluator(subjectEvaluator), toBooleanEvaluator(argEvaluators.get(0)));
            }
            case AND: {
                return new AndEvaluator(toBooleanEvaluator(subjectEvaluator), toBooleanEvaluator(argEvaluators.get(0)));
            }
            case NOT: {
                return new NotEvaluator(toBooleanEvaluator(subjectEvaluator));
            }
            default:
                throw new AttributeExpressionLanguageParsingException("Expected a Function-type expression but got " + tree.toString());
        }
    }

    public static class Range {

        private final int start;
        private final int end;

        public Range(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return start + " - " + end;
        }
    }
}

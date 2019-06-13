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
package org.apache.nifi.attribute.expression.language.evaluation.functions;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.nifi.attribute.expression.language.evaluation.EvaluatorState;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.WholeNumberEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.WholeNumberQueryResult;

public class OneUpSequenceEvaluator extends WholeNumberEvaluator {

    private static final AtomicLong value = new AtomicLong(0L);

    @Override
    public QueryResult<Long> evaluate(final Map<String, String> attributes, final EvaluatorState context) {
        return new WholeNumberQueryResult(value.getAndIncrement());
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return null;
    }

}

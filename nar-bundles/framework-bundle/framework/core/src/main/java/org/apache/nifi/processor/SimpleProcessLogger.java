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
package org.apache.nifi.processor;

import org.apache.nifi.logging.LogLevel;
import org.apache.nifi.logging.LogRepository;
import org.apache.nifi.logging.LogRepositoryFactory;
import org.apache.nifi.logging.ProcessorLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleProcessLogger implements ProcessorLog {

    private final Logger logger;
    private final LogRepository logRepository;
    private final Processor processor;

    public SimpleProcessLogger(final String processorId, final Processor processor) {
        this.logger = LoggerFactory.getLogger(processor.getClass());
        this.logRepository = LogRepositoryFactory.getRepository(processorId);
        this.processor = processor;
    }

    private Object[] addProcessor(final Object[] originalArgs) {
        return prependToArgs(originalArgs, processor);
    }

    private Object[] prependToArgs(final Object[] originalArgs, final Object... toAdd) {
        final Object[] newArgs = new Object[originalArgs.length + toAdd.length];
        System.arraycopy(toAdd, 0, newArgs, 0, toAdd.length);
        System.arraycopy(originalArgs, 0, newArgs, toAdd.length, originalArgs.length);
        return newArgs;
    }

    private Object[] translateException(final Object[] os) {
        if (os != null && os.length > 0 && (os[os.length - 1] instanceof Throwable)) {
            final Object[] osCopy = new Object[os.length];
            osCopy[osCopy.length - 1] = os[os.length - 1].toString();
            System.arraycopy(os, 0, osCopy, 0, os.length - 1);
            return osCopy;
        }
        return os;
    }

    private boolean lastArgIsException(final Object[] os) {
        return (os != null && os.length > 0 && (os[os.length - 1] instanceof Throwable));
    }

    @Override
    public void warn(final String msg, final Throwable t) {
        warn("{} " + msg, new Object[]{processor}, t);
    }

    @Override
    public void warn(String msg, Object[] os) {
        if (lastArgIsException(os)) {
            warn(msg, translateException(os), (Throwable) os[os.length - 1]);
        } else {
            msg = "{} " + msg;
            os = addProcessor(os);
            logger.warn(msg, os);
            logRepository.addLogMessage(LogLevel.WARN, msg, os);
        }
    }

    @Override
    public void warn(String msg, Object[] os, final Throwable t) {
        os = addProcessorAndThrowable(os, t);
        msg = "{} " + msg + ": {}";

        logger.warn(msg, os);
        if (logger.isDebugEnabled()) {
            logger.warn("", t);
        }
        logRepository.addLogMessage(LogLevel.WARN, msg, os, t);
    }

    @Override
    public void warn(String msg) {
        msg = "{} " + msg;
        final Object[] os = {processor};
        logger.warn(msg, processor);
        logRepository.addLogMessage(LogLevel.WARN, msg, os);
    }

    @Override
    public void trace(String msg, Throwable t) {
        msg = "{} " + msg;
        final Object[] os = {processor};
        logger.trace(msg, os, t);
        logRepository.addLogMessage(LogLevel.TRACE, msg, os, t);
    }

    @Override
    public void trace(String msg, Object[] os) {
        msg = "{} " + msg;
        os = addProcessor(os);
        logger.trace(msg, os);
        logRepository.addLogMessage(LogLevel.TRACE, msg, os);
    }

    @Override
    public void trace(String msg) {
        msg = "{} " + msg;
        final Object[] os = {processor};
        logger.trace(msg, os);
        logRepository.addLogMessage(LogLevel.TRACE, msg, os);
    }

    @Override
    public void trace(String msg, Object[] os, Throwable t) {
        os = addProcessorAndThrowable(os, t);
        msg = "{} " + msg + ": {}";

        logger.trace(msg, os);
        logger.trace("", t);
        logRepository.addLogMessage(LogLevel.TRACE, msg, os, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void info(String msg, Throwable t) {
        msg = "{} " + msg;
        final Object[] os = {processor};

        logger.info(msg, os);
        if (logger.isDebugEnabled()) {
            logger.info("", t);
        }
        logRepository.addLogMessage(LogLevel.INFO, msg, os, t);
    }

    @Override
    public void info(String msg, Object[] os) {
        msg = "{} " + msg;
        os = addProcessor(os);

        logger.info(msg, os);
        logRepository.addLogMessage(LogLevel.INFO, msg, os);
    }

    @Override
    public void info(String msg) {
        msg = "{} " + msg;
        final Object[] os = {processor};

        logger.info(msg, os);
        logRepository.addLogMessage(LogLevel.INFO, msg, os);
    }

    @Override
    public void info(String msg, Object[] os, Throwable t) {
        os = addProcessorAndThrowable(os, t);
        msg = "{} " + msg + ": {}";

        logger.info(msg, os);
        if (logger.isDebugEnabled()) {
            logger.info("", t);
        }
        logRepository.addLogMessage(LogLevel.INFO, msg, os, t);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public void error(String msg, Throwable t) {
        msg = "{} " + msg;
        final Object[] os = {processor};

        logger.error(msg, os, t);
        if (logger.isDebugEnabled()) {
            logger.error("", t);
        }
        logRepository.addLogMessage(LogLevel.ERROR, msg, os, t);
    }

    @Override
    public void error(String msg, Object[] os) {
        if (lastArgIsException(os)) {
            error(msg, translateException(os), (Throwable) os[os.length - 1]);
        } else {
            os = addProcessor(os);
            msg = "{} " + msg;
            logger.error(msg, os);
            logRepository.addLogMessage(LogLevel.ERROR, msg, os);
        }
    }

    @Override
    public void error(String msg) {
        msg = "{} " + msg;
        final Object[] os = {processor};

        logger.error(msg, os);
        logRepository.addLogMessage(LogLevel.ERROR, msg, os);
    }

    private Object[] addProcessorAndThrowable(final Object[] os, final Throwable t) {
        final Object[] modifiedArgs = new Object[os.length + 2];
        modifiedArgs[0] = processor.toString();
        for (int i = 0; i < os.length; i++) {
            modifiedArgs[i + 1] = os[i];
        }
        modifiedArgs[modifiedArgs.length - 1] = t.toString();

        return modifiedArgs;
    }

    @Override
    public void error(String msg, Object[] os, Throwable t) {
        os = addProcessorAndThrowable(os, t);
        msg = "{} " + msg + ": {}";

        logger.error(msg, os);
        if (logger.isDebugEnabled()) {
            logger.error("", t);
        }
        logRepository.addLogMessage(LogLevel.ERROR, msg, os, t);
    }

    @Override
    public void debug(String msg, Throwable t) {
        msg = "{} " + msg;
        final Object[] os = {processor};

        logger.debug(msg, os, t);
        logRepository.addLogMessage(LogLevel.DEBUG, msg, os, t);
    }

    /**
     *
     * @param msg
     * @param os
     */
    @Override
    public void debug(String msg, Object[] os) {
        os = addProcessor(os);
        msg = "{} " + msg;

        logger.debug(msg, os);
        logRepository.addLogMessage(LogLevel.DEBUG, msg, os);
    }

    @Override
    public void debug(String msg, Object[] os, Throwable t) {
        os = addProcessorAndThrowable(os, t);
        msg = "{} " + msg + ": {}";

        logger.debug(msg, os);
        if (logger.isDebugEnabled()) {
            logger.debug("", t);
        }
        logRepository.addLogMessage(LogLevel.DEBUG, msg, os, t);
    }

    @Override
    public void debug(String msg) {
        msg = "{} " + msg;
        final Object[] os = {processor};

        logger.debug(msg, os);
        logRepository.addLogMessage(LogLevel.DEBUG, msg, os);
    }

}

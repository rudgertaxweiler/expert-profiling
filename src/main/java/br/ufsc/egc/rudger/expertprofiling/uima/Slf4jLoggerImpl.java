package br.ufsc.egc.rudger.expertprofiling.uima;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;

import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.impl.Log4jLogger_impl;
import org.slf4j.LoggerFactory;

/**
 * UIMA Logger implementation that wraps an SLF4J Logger. Some code is kindly
 * borrowed from {@link Log4jLogger_impl}.
 * 
 */
public class Slf4jLoggerImpl implements Logger {

    public static void forceUsingThisImplementation() {
        System.setProperty("org.apache.uima.logger.class", Slf4jLoggerImpl.class.getName());
    }

    private static final String EXCEPTION_MESSAGE = "Exception occurred";

    private org.slf4j.Logger logger = null;

    /**
     * ResourceManager whose extension ClassLoader will be used to locate the
     * message digests. Null will cause the ClassLoader to default to
     * this.class.getClassLoader().
     */
    private ResourceManager mResourceManager = null;

    /**
     * create a new instance for the specified source class
     * 
     * @param component
     *            specified source class
     */
    private Slf4jLoggerImpl(final Class<?> component) {
        if (component != null) {
            this.logger = LoggerFactory.getLogger(component);
        } else {
            this.logger = LoggerFactory.getLogger("org.apache.uima");
        }
    }

    /**
     * create a new instance with the default logger from the Slf4j
     */
    private Slf4jLoggerImpl() {
        this(null);
    }

    public static synchronized Logger getInstance(final Class<?> component) {
        return new Slf4jLoggerImpl(component);
    }

    public static synchronized Logger getInstance() {
        return new Slf4jLoggerImpl();
    }

    /**
     * Logs a message with level INFO.
     * 
     * @deprecated use new function with log level
     * @param aMessage
     *            the message to be logged
     */
    @Override
    @Deprecated
    public void log(final String aMessage) {
        if (this.isLoggable(Level.INFO)) {
            if (aMessage == null || aMessage.equals("")) {
                return;
            }
            String[] sourceInfo = this.getStackTraceInfo(new Throwable());
            LoggerFactory.getLogger(sourceInfo[0]).info(aMessage);
        }
    }

    /**
     * Logs a message with a message key and the level INFO
     * 
     * @deprecated use new function with log level
     */
    @Override
    @Deprecated
    public void log(final String aResourceBundleName, final String aMessageKey,
            final Object[] aArguments) {
        if (this.isLoggable(Level.INFO)) {
            if (aMessageKey == null || aMessageKey.equals("")) {
                return;
            }

            String[] sourceInfo = this.getStackTraceInfo(new Throwable());
            LoggerFactory.getLogger(sourceInfo[0]).info(
                    I18nUtil.localizeMessage(aResourceBundleName, aMessageKey,
                            aArguments, this.getExtensionClassLoader()));
        }
    }

    /**
     * Logs an exception with level INFO
     * 
     * @deprecated use new function with log level
     * @param aException
     *            the exception to be logged
     */
    @Override
    @Deprecated
    public void logException(final Exception aException) {
        if (this.isLoggable(Level.INFO)) {
            if (aException == null) {
                return;
            }

            String[] sourceInfo = this.getStackTraceInfo(new Throwable());

            // log exception
            LoggerFactory.getLogger(sourceInfo[0]).info(EXCEPTION_MESSAGE, aException);
        }
    }

    /**
     * @deprecated use external configuration possibility
     */
    @Override
    @Deprecated
    public void setOutputStream(final OutputStream out) {
        throw new UnsupportedOperationException(
                "Method setOutputStream(OutputStream out) not supported");
    }

    /**
     * @deprecated use external configuration possibility
     */
    @Override
    @Deprecated
    public void setOutputStream(final PrintStream out) {
        throw new UnsupportedOperationException(
                "Method setOutputStream(PrintStream out) not supported");
    }

    @Override
    public boolean isLoggable(final Level level) {
        switch (level.toInteger()) {
        case Level.OFF_INT:
            return false;
        case Level.SEVERE_INT:
            return this.logger.isErrorEnabled();
        case Level.WARNING_INT:
            return this.logger.isWarnEnabled();
        case Level.INFO_INT:
        case Level.CONFIG_INT:
            return this.logger.isInfoEnabled();
        case Level.FINE_INT:
            return this.logger.isDebugEnabled();
        default:
            return this.logger.isTraceEnabled();
        }
    }

    @Override
    public void setLevel(final Level level) {
        // TODO use external adapters, e.g., for logback
        // for casting logger instance to its implementation
        // and using its methods
        System.err.println("Logging level changing is not implemented in Slf4jLoggerImpl");
    }

    private static void doLog(final org.slf4j.Logger logger, final Level level, final String msg) {
        switch (level.toInteger()) {
        case Level.OFF_INT:
            break;
        case Level.SEVERE_INT:
            logger.error(msg);
            break;
        case Level.WARNING_INT:
            logger.warn(msg);
            break;
        case Level.INFO_INT:
        case Level.CONFIG_INT:
            logger.info(msg);
            break;
        case Level.FINE_INT:
            logger.debug(msg);
            break;
        default:
            logger.trace(msg);
        }
    }

    private static void doLog(final org.slf4j.Logger logger, final Level level, final String msg, final Throwable thrown) {
        switch (level.toInteger()) {
        case Level.OFF_INT:
            break;
        case Level.SEVERE_INT:
            logger.error(msg, thrown);
            break;
        case Level.WARNING_INT:
            logger.warn(msg, thrown);
            break;
        case Level.INFO_INT:
        case Level.CONFIG_INT:
            logger.info(msg, thrown);
            break;
        case Level.FINE_INT:
            logger.debug(msg, thrown);
            break;
        default:
            logger.trace(msg, thrown);
        }
    }

    private void doLog(final Level level, final String msg) {
        doLog(this.logger, level, msg);
    }

    private void doLog(final Level level, final String msg, final Throwable thrown) {
        doLog(this.logger, level, msg, thrown);
    }

    @Override
    public void log(final Level level, final String aMessage) {
        if (this.isLoggable(level)) {
            if (aMessage == null || aMessage.equals("")) {
                return;
            }
            this.doLog(level, aMessage);
        }
    }

    @Override
    public void log(final Level level, final String aMessage, final Object param1) {
        if (this.isLoggable(level)) {
            if (aMessage == null || aMessage.equals("")) {
                return;
            }

            this.doLog(level, MessageFormat.format(aMessage,
                    new Object[] { param1 }));
        }
    }

    @Override
    public void log(final Level level, final String aMessage, final Object[] params) {
        if (this.isLoggable(level)) {
            if (aMessage == null || aMessage.equals("")) {
                return;
            }

            this.doLog(level, MessageFormat.format(aMessage, params));
        }
    }

    @Override
    public void log(final Level level, final String aMessage, final Throwable thrown) {
        if (this.isLoggable(level)) {
            if (aMessage != null && !aMessage.equals("")) {
                this.doLog(level, aMessage, thrown);
            }
            if (thrown != null && (aMessage == null || aMessage.equals(""))) {
                this.doLog(level, EXCEPTION_MESSAGE, thrown);
            }
        }

    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
            final String bundleName, final String msgKey, final Object param1) {
        if (this.isLoggable(level)) {
            if (msgKey == null || msgKey.equals("")) {
                return;
            }
            this.doLog(level, I18nUtil.localizeMessage(bundleName, msgKey,
                    new Object[] { param1 }, this.getExtensionClassLoader()));
        }
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
            final String bundleName, final String msgKey, final Object[] params) {
        if (this.isLoggable(level)) {
            if (msgKey == null || msgKey.equals("")) {
                return;
            }
            this.doLog(level, I18nUtil.localizeMessage(bundleName, msgKey,
                    params, this.getExtensionClassLoader()));
        }
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
            final String bundleName, final String msgKey, final Throwable thrown) {
        if (this.isLoggable(level)) {
            if (msgKey != null && !msgKey.equals("")) {
                this.doLog(level,
                        I18nUtil.localizeMessage(bundleName, msgKey, null,
                                this.getExtensionClassLoader()), thrown);
            }

            if (thrown != null && (msgKey == null || msgKey.equals(""))) {
                this.doLog(level, EXCEPTION_MESSAGE, thrown);
            }
        }
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
            final String bundleName, final String msgKey) {
        if (this.isLoggable(level)) {

            if (msgKey == null || msgKey.equals("")) {
                return;
            }

            this.doLog(level,
                    I18nUtil.localizeMessage(bundleName, msgKey, null,
                            this.getExtensionClassLoader()));
        }
    }

    @Override
    public void log(final String wrapperFQCN, final Level level, final String message, final Throwable thrown) {
        this.log(level, message, thrown);
    }

    @Override
    public void setResourceManager(final ResourceManager resourceManager) {
        this.mResourceManager = resourceManager;
    }

    /**
     * Gets the extension ClassLoader to used to locate the message digests. If
     * this returns null, then message digests will be searched for using
     * this.class.getClassLoader().
     */
    private ClassLoader getExtensionClassLoader() {
        if (this.mResourceManager == null) {
            return null;
        } else {
            return this.mResourceManager.getExtensionClassLoader();
        }
    }

    /**
     * returns the method name and the line number if available
     * 
     * @param thrown
     *            the thrown
     * @return String[] - fist element is the souce class, second element is the
     *         method name with linenumber if available
     */
    private String[] getStackTraceInfo(final Throwable thrown) {
        StackTraceElement[] stackTraceElement = thrown.getStackTrace();

        String sourceMethod = "";
        String sourceClass = "";
        int lineNumber = 0;
        try {
            lineNumber = stackTraceElement[1].getLineNumber();
            sourceMethod = stackTraceElement[1].getMethodName();
            sourceClass = stackTraceElement[1].getClassName();
        } catch (Exception ex) {
            // do nothing, use the initialized string members
        }

        if (lineNumber > 0) {
            StringBuffer buffer = new StringBuffer(25);
            buffer.append(sourceMethod);
            buffer.append("(");
            buffer.append(lineNumber);
            buffer.append(")");
            sourceMethod = buffer.toString();
        }

        return new String[] { sourceClass, sourceMethod };
    }
}
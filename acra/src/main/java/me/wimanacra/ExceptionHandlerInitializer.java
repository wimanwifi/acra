package me.wimanacra;

import me.wimanacra.builder.ReportPrimer;

/**
 * The interface can be used with
 * {@link ErrorReporter#setExceptionHandlerInitializer(ExceptionHandlerInitializer)}
 * to add an additional initialization of the {@link ErrorReporter} before
 * exception is handled.
 * 
 * @see ErrorReporter#setExceptionHandlerInitializer(ExceptionHandlerInitializer)
 * @deprecated since 4.8.0 use {@link ReportPrimer} mechanism instead.
 */
public interface ExceptionHandlerInitializer {
    /**
     * Called before {@link ErrorReporter} handles the Exception.
     * 
     * @param reporter The {@link ErrorReporter} that will handle the exception
     */
    void initializeExceptionHandler(ErrorReporter reporter);
}
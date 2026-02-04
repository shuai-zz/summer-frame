package org.example.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhaoshuai
 */
@FunctionalInterface
public interface InputStreamCallback<T> {
    T doWithInputStream(InputStream inputStream) throws IOException;
}

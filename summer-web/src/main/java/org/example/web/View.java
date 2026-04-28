package org.example.web;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * @author zhaoshuai
 */
public interface View {

    @Nullable
    default String getContentType(){
        return null;
    }

    void render(Map<String, Object> model, HttpServletRequest req, HttpServletResponse resp) throws Exception;
}

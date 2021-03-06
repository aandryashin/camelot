package ru.yandex.qatools.camelot.core.web.jackson;

import java.io.IOException;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
public interface JsonSerializer {
    String toJson(Object instance) throws IOException;
}

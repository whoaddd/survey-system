package com.survey.common;

import java.io.Serializable;
import java.util.List;

public record PageResult<T>(List<T> list, long total, int page, int size)
        implements Serializable {
}
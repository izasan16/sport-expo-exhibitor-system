package com.sportexpo.exhibitor.common;

import java.util.List;

public record PageResponse<T>(List<T> records, long total, int page, int pageSize) {
}

package com.sleekydz86.catalog.domain.extract.service;

import com.sleekydz86.catalog.domain.extract.model.ExtractQueryCommand;
import com.sleekydz86.catalog.domain.extract.model.ValidatedExtractQuery;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public interface ExtractQueryService {

    ValidatedExtractQuery build(ExtractQueryCommand command, int maxRows, Duration timeout);
}

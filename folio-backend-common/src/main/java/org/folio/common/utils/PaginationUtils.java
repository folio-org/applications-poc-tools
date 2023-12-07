package org.folio.common.utils;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

@Log4j2
@UtilityClass
public class PaginationUtils {

  public <T> List<T> loadInBatches(List<String> ids, Function<List<String>, List<T>> queryFunction, Integer batchSize) {
    if (batchSize < 1) {
      throw new IllegalArgumentException("Batch size should be >= 1");
    }

    var offset = 0;
    int limit = ids.size();
    var allRecords = new ArrayList<T>();

    log.debug("Loading data in batches: limit = {}, batchSize = {}", limit, batchSize);
    do {
      log.debug("Loading batch data at offset: offset = {}", offset);
      var idsBatch = subListAtOffset(offset, batchSize, ids);
      if (idsBatch.isEmpty()) {
        return emptyList();
      }
      log.debug("Current values for batch loading: values = {}", idsBatch);

      var result = queryFunction.apply(idsBatch);
      offset += batchSize;
      allRecords.addAll(result);
    } while (offset < limit);

    return allRecords;
  }

  public static <T> List<T> subListAtOffset(Integer offset, Integer limit, List<T> list) {
    if (CollectionUtils.isEmpty(list)) {
      return list;
    }
    int sublistToIndex = Math.min(offset + limit, list.size());
    int sublistFromIndex = Math.min(offset, sublistToIndex);
    return list.subList(sublistFromIndex, sublistToIndex);
  }
}

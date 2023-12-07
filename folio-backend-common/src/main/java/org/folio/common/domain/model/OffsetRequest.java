package org.folio.common.domain.model;

import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

@EqualsAndHashCode
@ToString
public class OffsetRequest implements Pageable {

  public static final Sort DEFAULT_SORT = Sort.by(Direction.ASC, "id");

  private final long offset;
  private final int limit;
  private final Sort sort;

  public OffsetRequest(long offset, int limit) {
    this(offset, limit, DEFAULT_SORT);
  }

  public OffsetRequest(long offset, int limit, Sort sort) {
    Assert.isTrue(offset >= 0, "Negative offset is not allowed: " + offset);
    Assert.isTrue(limit > 0, "Limit cannot be negative or zero: " + limit);
    this.offset = offset;
    this.limit = limit;

    this.sort = Objects.requireNonNull(sort, "Sorting cannot be null. Use Sort.unsorted() instead");
  }

  public static OffsetRequest of(int offset, int limit) {
    return of(offset, limit, DEFAULT_SORT);
  }

  public static OffsetRequest of(int offset, int limit, Sort sort) {
    return new OffsetRequest(offset, limit, sort);
  }

  @Override
  public int getPageNumber() {
    return Math.toIntExact(offset / limit);
  }

  @Override
  public int getPageSize() {
    return limit;
  }

  @Override
  public long getOffset() {
    return offset;
  }

  @Override
  @NonNull
  public Sort getSort() {
    return sort;
  }

  @Override
  @NonNull
  public Pageable next() {
    return new OffsetRequest(getOffset() + getPageSize(), getPageSize(), getSort());
  }

  public Pageable previous() {
    return hasPrevious() ? new OffsetRequest(getOffset() - getPageSize(), getPageSize(), getSort()) : this;
  }

  @Override
  @NonNull
  public Pageable previousOrFirst() {
    return hasPrevious() ? previous() : first();
  }

  @Override
  @NonNull
  public Pageable first() {
    return new OffsetRequest(0, getPageSize(), getSort());
  }

  @Override
  @NonNull
  public Pageable withPage(int pageNumber) {
    return new OffsetRequest((long) pageNumber * getPageSize(), getPageSize(), getSort());
  }

  @Override
  public boolean hasPrevious() {
    return offset > limit;
  }
}

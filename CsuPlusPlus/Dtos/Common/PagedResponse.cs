namespace CsuPlusPlus.Dtos.Common;

public record PagedResponse<T>(
    IReadOnlyList<T> Items,
    int Page,
    int PageSize,
    long TotalCount
)
{
    public long TotalPages => (TotalCount + PageSize - 1) / PageSize;

    public static PagedResponse<T> EmptyPage => new([], Page: 1, PageSize: 1, TotalCount: 0);
}
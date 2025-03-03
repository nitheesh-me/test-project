package com.sismics.reader.core.service;


import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.dao.jpa.dto.UserArticleDto;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;

public interface IIndexingService {
    PaginatedList<UserArticleDto> searchArticles(String userId, String searchQuery, Integer offset, Integer limit) throws Exception;
    void rebuildIndex() throws Exception;
    Directory getDirectory();
    DirectoryReader getDirectoryReader();
}
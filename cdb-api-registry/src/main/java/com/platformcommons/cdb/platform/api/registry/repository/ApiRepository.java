package com.platformcommons.cdb.platform.api.registry.repository;

import com.platformcommons.cdb.platform.api.registry.model.Api;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository abstraction for persisting and retrieving Api entities.
 */
@Repository
public interface ApiRepository extends JpaRepository<Api, Long>, JpaSpecificationExecutor<Api> {

    Page<Api> findAllByCreatedByProvider(Pageable pageable,Long currentProviderId);
    
    
    @Query("SELECT DISTINCT a FROM Api a LEFT JOIN a.tags t LEFT JOIN a.domains d WHERE " +
           "(:query IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT( :query, '%')) OR " +
           "LOWER(a.owner) LIKE LOWER(CONCAT( :query, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT(:query, '%')) OR " +
           "LOWER(t) LIKE LOWER(CONCAT(:query, '%')) OR " +
           "LOWER(d) LIKE LOWER(CONCAT(:query, '%'))) AND " +
           "(:tags IS NULL OR t IN :tags) AND " +
           "(:domains IS NULL OR d IN :domains) AND " +
           "(:owner IS NULL OR LOWER(a.owner) LIKE LOWER(CONCAT(:owner, '%'))) AND " +
           "(:statusList IS NULL OR a.status IN :statusList) AND " +
           "a.isActive = true")
    Page<Api> searchApis(@Param("query") String query,
                        @Param("tags") List<String> tags,
                        @Param("domains") List<String> domains,
                        @Param("owner") String owner,
                        @Param("statusList") List<String> statusList,
                        Pageable pageable);

    @Query("SELECT d, COUNT(DISTINCT a.id) FROM Api a JOIN a.domains d WHERE a.isActive = true AND a.status != 'DRAFT' GROUP BY d ORDER BY COUNT(DISTINCT a.id) DESC")
    List<Object[]> findDomainStats();

    Page<Api> findByDomainsContaining(String domain, Pageable pageable);

    @Query("SELECT a FROM Api a WHERE :domain MEMBER OF a.domains AND a.isActive = true AND a.status != :excludeStatus")
    Page<Api> findByDomainsContainingAndStatusNot(@Param("domain") String domain, @Param("excludeStatus") String excludeStatus, Pageable pageable);

    @Query("SELECT DISTINCT t FROM Api a JOIN a.tags t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY t")
    List<String> findDistinctTagsContaining(@Param("query") String query);

    @Query("SELECT DISTINCT d FROM Api a JOIN a.domains d WHERE LOWER(d) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY d")
    List<String> findDistinctDomainsContaining(@Param("query") String query);

    @Query("SELECT DISTINCT t FROM Api a JOIN a.tags t WHERE a.isActive = true AND a.status != 'DRAFT' ORDER BY t")
    List<String> findDistinctTagsByActiveApis(Pageable pageable);

    @Query("SELECT DISTINCT d FROM Api a JOIN a.domains d WHERE a.isActive = true AND a.status != 'DRAFT' ORDER BY d")
    List<String> findDistinctDomainsByActiveApis(Pageable pageable);

    @Query("SELECT DISTINCT a.owner FROM Api a WHERE a.isActive = true AND a.status != 'DRAFT' ORDER BY a.owner")
    List<String> findDistinctOwnersByActiveApis(Pageable pageable);

    @Query("SELECT DISTINCT t FROM Api a JOIN a.tags t WHERE a.isActive = true AND a.status != 'DRAFT' AND LOWER(t) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY t")
    List<String> findDistinctTagsByActiveApisWithSearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT DISTINCT d FROM Api a JOIN a.domains d WHERE a.isActive = true AND a.status != 'DRAFT' AND LOWER(d) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY d")
    List<String> findDistinctDomainsByActiveApisWithSearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT DISTINCT a.owner FROM Api a WHERE a.isActive = true AND a.status != 'DRAFT' AND LOWER(a.owner) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY a.owner")
    List<String> findDistinctOwnersByActiveApisWithSearch(@Param("search") String search, Pageable pageable);
}

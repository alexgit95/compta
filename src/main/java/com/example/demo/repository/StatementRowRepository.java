package com.example.demo.repository;

import com.example.demo.model.StatementRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StatementRowRepository extends JpaRepository<StatementRow, Long> {
    interface ImportedPeriod {
        int getYear();
        int getMonth();
    }

    @Query("select r.year as year, r.month as month from StatementRow r where r.kind = :kind "
            + "group by r.year, r.month order by r.year desc, r.month desc")
    List<ImportedPeriod> findImportedPeriods(@Param("kind") StatementRow.Kind kind);

    List<StatementRow> findByKindAndYearAndMonthOrderByDateAscLineNumberAsc(StatementRow.Kind kind, int year, int month);

    List<StatementRow> findAllByOrderByYearDescMonthDescLineNumberAsc();
    List<StatementRow> findByYearAndMonthOrderByLineNumberAsc(int year, int month);
    void deleteByYearAndMonth(int year, int month);
}
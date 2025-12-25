package com.chronos.chronos.repository;

import com.chronos.chronos.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    // custom queries can be added here later
}


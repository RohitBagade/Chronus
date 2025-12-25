package com.chronos.chronos.repository;

import com.chronos.chronos.entity.JobExecution;
import com.chronos.chronos.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {
    List<JobExecution> findByJob(Job job);
}

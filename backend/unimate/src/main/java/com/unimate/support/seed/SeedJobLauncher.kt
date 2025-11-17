package com.unimate.support.seed

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("seed")
class SeedJobLauncher(
    private val jobLauncher: JobLauncher,
    private val seedJob: Job
) : ApplicationRunner {

    @Throws(Exception::class)
    override fun run(args: ApplicationArguments) {
        val params = JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .toJobParameters()
        jobLauncher.run(seedJob, params)
    }
}


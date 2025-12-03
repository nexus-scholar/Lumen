package com.lumen.core.di

import com.lumen.core.data.repository.InMemoryProjectRepository
import com.lumen.core.domain.repository.ProjectRepository
import com.lumen.core.domain.usecase.CreateProjectUseCase
import org.koin.dsl.module

/**
 * Dependency injection module for core domain layer (common)
 */
val coreModule = module {
    // Repositories
    single<ProjectRepository> { InMemoryProjectRepository() }

    // Use Cases
    factory { CreateProjectUseCase(get()) }
}


package com.example.backend.service;

import com.example.backend.model.ParametersBool;
import com.example.backend.repository.ParametersBoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ParametersBoolService {

    private static final Logger logger = LoggerFactory.getLogger(ParametersBoolService.class);
    @Autowired
    private ParametersBoolRepository repository;

    @PostConstruct
    public void initializeDefaultParameters() {
        checkAndCreateParameter("free_access", true);
    }

    private void checkAndCreateParameter(String name, boolean defaultValue) {
        if (!repository.existsByName(name)) {
            ParametersBool parameter = new ParametersBool();
            parameter.setName(name);
            parameter.setValue(defaultValue);
            repository.save(parameter);
            logger.info("Utworzono parametr: {} z wartością: {}", name, defaultValue);
        }
    }

    public boolean deleteParameter(String name) {
        Optional<ParametersBool> parameter = Optional.ofNullable(repository.findByName(name));
        if (parameter.isPresent()) {
            repository.delete(parameter.get());
            logger.info("Usunięto parametr: {}", name);
            return true;
        }
        return false;
    }

    public List<ParametersBool> getAllParameters() {
        return repository.findAll();
    }

    public Optional<ParametersBool> findByName(String name) {
        return Optional.ofNullable(repository.findByName(name));
    }

    public void updateParameter(String name, boolean value) {
        ParametersBool parameter = repository.findByName(name);
        if (parameter != null) {
            parameter.setValue(value);
            repository.save(parameter);
            logger.info("Zaktualizowano parametr: {} na wartość: {}", name, value);
        }
    }

    public Boolean getParameterValue(String name) {
        ParametersBool parameter = repository.findByName(name);
        return parameter != null ? parameter.getValue() : null;
    }
}
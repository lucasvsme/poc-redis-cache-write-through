package com.example.person;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PersonServiceDefault implements PersonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonServiceDefault.class);

    private final PersonRepository personRepository;
    private final Cache cache;

    public PersonServiceDefault(PersonRepository personRepository, CacheManager cacheManager) {
        this.personRepository = personRepository;
        this.cache = cacheManager.getCache("redis");
    }

    @Override
    @Transactional
    public Person create(String name, Integer age) {
        final var person = new Person();
        person.setId(UUID.randomUUID());
        person.setName(name);
        person.setAge(age);

        cache.put(person.getId(), person);
        LOGGER.info("Person cached (key={}, value={})", person.getId(), person);

        personRepository.save(person);
        LOGGER.info("Person saved (person={})", person);

        return person;
    }

    @Override
    public Person findOne(UUID personId) throws PersonNotFoundException {
        final var personOnCache = cache.get(personId, Person.class);
        if (personOnCache != null) {
            LOGGER.info("Person retrieved from cache (personId={})", personId);
            return personOnCache;
        }

        final var personNotCached = personRepository.findById(personId);
        if (personNotCached.isPresent()) {
            LOGGER.info("Person retrieved from database (personId={})", personId);

            final var person = personNotCached.get();
            cache.put(personId, person);
            LOGGER.info("Person cached (key={}, value={})", personId, person);

            return person;
        }

        LOGGER.info("Person not found (personId={})", personId);
        throw new PersonNotFoundException(personId);
    }
}

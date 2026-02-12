package com.github.rylxes.softdelete;

import com.github.rylxes.softdelete.config.SoftDeleteAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@Import(SoftDeleteAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SoftDeleteIntegrationTest {

    @Autowired
    private TestEntityRepository repository;

    private TestEntity alice;
    private TestEntity bob;
    private TestEntity charlie;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        alice = repository.save(new TestEntity("Alice"));
        bob = repository.save(new TestEntity("Bob"));
        charlie = repository.save(new TestEntity("Charlie"));
    }

    // ── Soft Delete ──────────────────────────────────────────

    @Nested
    @DisplayName("softDelete()")
    class SoftDeleteTests {

        @Test
        @DisplayName("sets deletedAt and excludes from findAll")
        void softDelete_setsTimestamp_and_excludesFromFindAll() {
            repository.softDelete(alice);

            assertTrue(alice.isDeleted(), "Entity should be marked as deleted");
            assertNotNull(alice.getDeletedAt());

            List<TestEntity> results = repository.findAll();
            assertEquals(2, results.size(), "Soft-deleted entity should not appear in findAll");
            assertTrue(results.stream().noneMatch(e -> e.getName().equals("Alice")));
        }

        @Test
        @DisplayName("softDeleteById works")
        void softDeleteById_works() {
            repository.softDeleteById(bob.getId());

            List<TestEntity> results = repository.findAll();
            assertEquals(2, results.size());
            assertTrue(results.stream().noneMatch(e -> e.getName().equals("Bob")));
        }
    }

    // ── Restore ──────────────────────────────────────────────

    @Nested
    @DisplayName("restore()")
    class RestoreTests {

        @Test
        @DisplayName("restore clears deletedAt and includes in findAll again")
        void restore_entityReappearsInFindAll() {
            repository.softDelete(alice);
            assertEquals(2, repository.findAll().size());

            repository.restore(alice);

            assertFalse(alice.isDeleted());
            assertNull(alice.getDeletedAt());
            assertEquals(3, repository.findAll().size());
        }

        @Test
        @DisplayName("restoreById works")
        void restoreById_works() {
            repository.softDelete(alice);
            repository.restoreById(alice.getId());
            assertEquals(3, repository.findAll().size());
        }
    }

    // ── Force Delete ─────────────────────────────────────────

    @Nested
    @DisplayName("forceDelete()")
    class ForceDeleteTests {

        @Test
        @DisplayName("permanently removes the entity")
        void forceDelete_permanentlyRemoves() {
            repository.forceDelete(alice);

            assertEquals(2, repository.findAll().size());
            assertEquals(2, repository.findAllWithTrashed().size(),
                    "Force-deleted entity should not exist even in withTrashed");
        }

        @Test
        @DisplayName("forceDeleteById works on soft-deleted entity")
        void forceDeleteById_works() {
            repository.softDelete(bob);
            repository.forceDeleteById(bob.getId());

            assertEquals(2, repository.findAll().size());
            assertEquals(2, repository.findAllWithTrashed().size());
        }
    }

    // ── Query Scopes ─────────────────────────────────────────

    @Nested
    @DisplayName("Query Scopes")
    class QueryScopeTests {

        @Test
        @DisplayName("findAllWithTrashed returns active + deleted")
        void findAllWithTrashed_returnsAll() {
            repository.softDelete(alice);
            repository.softDelete(bob);

            List<TestEntity> all = repository.findAllWithTrashed();
            assertEquals(3, all.size());
        }

        @Test
        @DisplayName("findByIdWithTrashed finds a deleted entity")
        void findByIdWithTrashed_findsDeleted() {
            repository.softDelete(alice);

            Optional<TestEntity> found = repository.findByIdWithTrashed(alice.getId());
            assertTrue(found.isPresent());
            assertEquals("Alice", found.get().getName());
        }

        @Test
        @DisplayName("findAllTrashed returns only deleted entities")
        void findAllTrashed_returnsOnlyDeleted() {
            repository.softDelete(alice);
            repository.softDelete(charlie);

            List<TestEntity> trashed = repository.findAllTrashed();
            assertEquals(2, trashed.size());
            assertTrue(trashed.stream().allMatch(SoftDeletable::isDeleted));
        }

        @Test
        @DisplayName("countWithTrashed counts everything")
        void countWithTrashed_countsAll() {
            repository.softDelete(alice);
            assertEquals(3, repository.countWithTrashed());
        }

        @Test
        @DisplayName("countTrashed counts only deleted")
        void countTrashed_countsDeleted() {
            repository.softDelete(alice);
            repository.softDelete(bob);
            assertEquals(2, repository.countTrashed());
        }
    }

    // ── findAll auto-filter ──────────────────────────────────

    @Nested
    @DisplayName("Auto-filter")
    class AutoFilterTests {

        @Test
        @DisplayName("standard findAll excludes soft-deleted")
        void findAll_excludesDeleted() {
            repository.softDelete(alice);
            List<TestEntity> results = repository.findAll();
            assertEquals(2, results.size());
            assertTrue(results.stream().noneMatch(e -> e.getName().equals("Alice")));
        }

        @Test
        @DisplayName("standard findById returns empty for soft-deleted")
        void findById_returnsEmpty_forDeleted() {
            repository.softDelete(alice);
            Optional<TestEntity> found = repository.findById(alice.getId());
            assertTrue(found.isEmpty(), "findById should not find soft-deleted entity");
        }

        @Test
        @DisplayName("standard count excludes soft-deleted")
        void count_excludesDeleted() {
            repository.softDelete(alice);
            repository.softDelete(bob);
            assertEquals(1, repository.count());
        }
    }

    // ── isDeleted helper ─────────────────────────────────────

    @Test
    @DisplayName("isDeleted returns correct state")
    void isDeleted_returnsCorrectState() {
        assertFalse(alice.isDeleted());
        repository.softDelete(alice);
        assertTrue(alice.isDeleted());
        repository.restore(alice);
        assertFalse(alice.isDeleted());
    }
}

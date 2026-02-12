# Spring Boot Soft Delete

**Laravel-style soft deletes for Spring Boot JPA** — database agnostic, plug-and-play.

[![Java 17+](https://img.shields.io/badge/java-17%2B-blue)](https://openjdk.org)
[![Spring Boot 3.x](https://img.shields.io/badge/spring--boot-3.x-green)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/license-MIT-yellow)](LICENSE)

---

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>com.github.rylxes</groupId>
    <artifactId>spring-boot-softdelete</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or install locally first:

```bash
mvn clean install
```

### 2. Make your entity soft-deletable

```java
@Entity
public class Post extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // getters, setters…
}
```

### 3. Use the soft-delete repository

```java
public interface PostRepository extends SoftDeleteRepository<Post, Long> {
}
```

### 4. Use it!

```java
@Service
public class PostService {

    @Autowired
    private PostRepository posts;

    public void archive(Long id) {
        posts.softDeleteById(id);           // Sets deleted_at = now()
    }

    public void restore(Long id) {
        posts.restoreById(id);              // Clears deleted_at
    }

    public void permanentlyDelete(Long id) {
        posts.forceDeleteById(id);          // Actual DELETE from DB
    }

    public List<Post> getActive() {
        return posts.findAll();             // Auto-excludes soft-deleted
    }

    public List<Post> getAll() {
        return posts.findAllWithTrashed();  // Active + deleted
    }

    public List<Post> getTrash() {
        return posts.findAllTrashed();      // Only deleted
    }
}
```

---

## Configurable Column Name

### Per-entity override

```java
@Entity
@SoftDeleteColumn("removed_at")
public class Invoice extends SoftDeletableEntity {
    // uses "removed_at" column instead of "deleted_at"
}
```

### Global default via properties

```properties
# application.properties
softdelete.column-name=removed_at
```

> Per-entity `@SoftDeleteColumn` takes precedence over the global property.

---

## API Reference

| Method | Description | Laravel Equivalent |
|---|---|---|
| `softDelete(entity)` | Set `deleted_at = now()` | `$model->delete()` |
| `softDeleteById(id)` | Same, by id | — |
| `restore(entity)` | Clear `deleted_at` | `$model->restore()` |
| `restoreById(id)` | Same, by id | — |
| `forceDelete(entity)` | Permanent DB delete | `$model->forceDelete()` |
| `forceDeleteById(id)` | Same, by id | — |
| `findAll()` | Active only (auto-filtered) | `Model::all()` |
| `findAllWithTrashed()` | Active + deleted | `Model::withTrashed()->get()` |
| `findByIdWithTrashed(id)` | Find including deleted | `Model::withTrashed()->find()` |
| `findAllTrashed()` | Only deleted | `Model::onlyTrashed()->get()` |
| `count()` | Count active only | `Model::count()` |
| `countWithTrashed()` | Count all | `Model::withTrashed()->count()` |
| `countTrashed()` | Count deleted only | `Model::onlyTrashed()->count()` |
| `entity.isDeleted()` | Check if soft-deleted | `$model->trashed()` |

---

## How It Works

1. **`SoftDeletableEntity`** is a `@MappedSuperclass` with a `deleted_at` column and a Hibernate `@FilterDef`.
2. **`SoftDeleteFilterAspect`** (AOP) enables the Hibernate filter before every repository call, so `findAll()`, `findById()`, `count()` etc. automatically add `WHERE deleted_at IS NULL`.
3. **`SoftDeleteRepositoryImpl`** temporarily disables the filter for `withTrashed` and `onlyTrashed` queries, then re-enables it.
4. **Auto-configuration** wires everything when the jar is on the classpath — zero config needed.

---

## Deploy to Maven Repo

```bash
# Install locally
mvn clean install

# Deploy to a remote Maven repository (configure <distributionManagement> in pom.xml)
mvn clean deploy
```

The POM is pre-configured with `maven-source-plugin` and `maven-javadoc-plugin` to produce `-sources.jar` and `-javadoc.jar` alongside the main artifact.

---

## License

MIT

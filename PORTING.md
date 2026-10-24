# Hibernate 7 port

This branch (`hibernate-7`) ports AutoFetch (Ibrahim and Cook, ECOOP 2006) to Hibernate ORM 7.4 and Java 17+.
It starts from the last commit of the `5.3.0` branch of
[ErikJonasson/Autofetch](https://github.com/ErikJonasson/Autofetch) (`5a9baa7`, 2018-10-01), the Hibernate 5.3
migration by Jonasson. It was made for the ORM anti-pattern detection benchmark
(`orm-anti-pattern-detection`), which evaluates the published version and this port as two separate conditions.

AutoFetch keeps its license: see `COPYING.LESSER` (LGPL 2.1). The source headers state the Lesser GPL version 3
or later; every ported file keeps its header, and each changed file says what the port changed in it.

## What did not change

The algorithm is the published code, unchanged:

- `ExtentManager`, `TraversalProfile`, `Statistics`, `Path`, `ProgramStack`: traversal profiles keyed by the query
  and the program stack, the 0.4 access-frequency threshold, and the maximum prefetch depth of 5;
- `EntityTracker` and `CollectionTracker`: when an entity or a collection counts as accessed, and how a profile is
  extended to its associations;
- `Trackable`, `TrackableEntity`, `Property`.

The one change inside these classes is in `EntityTracker.extendProfile`, for lazy to-one proxies (see below).

## What changed, and why

Hibernate 6 and 7 removed every extension point the 5.3 version was built on. Each mechanism is re-hosted on a
public Hibernate 7 SPI:

| Mechanism | Hibernate 5.3 version | Hibernate 7 port |
|---|---|---|
| Tracking entity instances | `AutofetchTuplizer` + `AutofetchInstantiator` create Javassist subclasses | Tuplizers were removed and the representation strategy is not pluggable. `AutofetchSessionFactoryBuilderFactory` installs a session-scoped `AutofetchInterceptor`, whose `instantiate` method Hibernate calls before the default constructor; `EntityProxyFactory` generates the subclasses with ByteBuddy. `EntityProxyMethodHandler` keeps the same dispatch. |
| Tracking lazy to-one proxies | `AutofetchProxyFactory` + `AutofetchLazyInitializer` make Hibernate's proxies `Trackable` | Hibernate 7 creates its proxies itself. `EntityTracker` keeps the tracker of an uninitialized proxy in the session's `AutofetchInterceptor`, which adds it to the entity when the proxy is initialized and the entity instantiated. |
| Tracking collections | `AutofetchMetadataContributor` replaces bag, list, set, and identifier bag types | `AutofetchMappingContributor` (an `AdditionalMappingContributor`) replaces bag, list, and set types with the same wrappers on the Hibernate 7 `PersistentCollection` API. Identifier bags keep their standard type and are not tracked. |
| Prefetching on queries | `AutofetchCriteria` wraps a legacy `Criteria` and sets `FetchMode.JOIN` per path | The legacy Criteria API was removed. `AutofetchQuery` wraps a JPA `TypedQuery` (JPQL, HQL, or a JPA `CriteriaQuery`) that selects its single root entity, and applies the paths as a load graph (`jakarta.persistence.loadgraph`). |
| Prefetching on `load`/`get` | `AutofetchLoadListener` | The same listener on the Hibernate 7 `DefaultLoadEventListener`; its HQL uses an explicit `select` and `id()`. |
| Prefetching on collection initialization | `AutofetchInitializeCollectionListener` | The same listener; when the collection has no prefetch paths, or is in the second-level cache, it delegates to the default listener instead of copying the cache lookup. |
| Registration | `auto-service` annotations | Explicit `META-INF/services` files. |

Two behaviors of the 5.3 version are not carried over, because the Hibernate 7 APIs do not have them:

- `AutofetchCriteria.add` and `addOrder` returned the unwrapped `Criteria`, so a fluent call chain ran without
  AutoFetch. `AutofetchQuery` wraps a query that is already configured, so there is no such chain.
- A legacy `Criteria` with a collection join fetch returned one root per row. Hibernate 7 returns each root
  once, so the prefetch no longer changes the query result. In the 5.3 version, the duplicates also counted as
  roots in the statistics, which made prefetching switch off and on periodically.

Two defects of the 5.3 version, found by running it on the PetClinic-derived set, do not occur in the port. Both
break PetClinic's write paths, with prefetching on or off:

- `AutofetchBagType.getElementsIterator` and `contains` cast the bag to `Set`, so any cascade over a bag mapping (a
  `List` without an order column, like PetClinic's `Owner.pets`) throws a `ClassCastException`. The port's collection
  types treat the collection as a `Collection`.
- Merging a detached entity fails with `Unknown entity: Owner_$$_jvst...`, because Hibernate 5.3 cannot resolve a
  Javassist tracking subclass to its entity. The port's `AutofetchInterceptor.getEntityName` maps its subclasses back
  to their entity names.

One defect of the 5.3 version is kept, because the port keeps the published collection types: their
`replaceElements` clears the managed collection and adds the detached elements of the merged one, instead of merging
them, so merging an entity whose collection holds other entities fails at flush with `Detached entity passed to
persist`. On the PetClinic-derived set this breaks the same four merges in both versions; in the 5.3 version, the
`Unknown entity` error comes first.

## Tests

The original `ExtentTest` is kept, with only the API changes Hibernate 7 requires: `Session.load` becomes
`getReference`, `Session.save` becomes `persist`, the `Criteria` queries become JPQL in `AutofetchQuery`, and the
hibernate-testing base class becomes a JPA 3.2 bootstrap on H2 (`BaseEntityManagerFunctionalTestCase`).

| Branch | Hibernate | Failing tests (of 17) |
|---|---|---|
| `master` (upstream) | 4.3.10 | 4: `testLoadFetchProfile`, `testLoadFetchProfileCriteria`, `testSecondLevelSupervisorAccess`, `testSupervisorAndMentorAccess` |
| `5.3.0` (upstream) | 5.3.1 | 9: `testCollectionAccess2ndLevel1`, `testMentorAccess`, `testMentorAccessAggregate1`, `testMentorAccessAggregate2`, `testSecondLevelSupervisorAccess`, `testSupervisorAndMentorAccess`, `testLoadFetchProfileCriteria`, `testMultipleCollectionFetch`, `testMultipleCollectionFetchCriteria` |
| `hibernate-7` (this port) | 7.4.13 | 5: `testLoadFetchProfile`, `testSecondLevelSupervisorAccess`, `testSupervisorAndMentorAccess`, `testMultipleCollectionFetch`, `testMultipleCollectionFetchCriteria` |

Every test that fails on this port also fails on an upstream branch:

- `testSecondLevelSupervisorAccess` and `testSupervisorAndMentorAccess` pass their assertions on the
  `m_supervisor` statistics, then ask for sub-profiles named `supervisor` and `subordinates`, which the test model
  does not have (its properties are `m_supervisor` and `m_subordinates`).
- `testMultipleCollectionFetch` and `testMultipleCollectionFetchCriteria` traverse a new object graph once and
  then load three more graphs without traversing them, and expect the last one to be prefetched. They fail with the
  same `LazyInitializationException` ("could not initialize proxy ... no session") on the `5.3.0` branch, whose
  test entities, like this port's, use field access. The cause has not been isolated; a likely one is that the runs
  without traversal add to the associations' totals but not to their accesses, which lowers the access ratio
  toward the 0.4 threshold.
- `testLoadFetchProfile` expects a `LazyInitializationException` for an association it never traversed, but
  reading a detached uninitialized proxy reference does not throw; it fails with the same message on `master`.

## Build

```bash
mvn clean install -DskipTests    # Java 17 or later; the tests need no database (H2)
mvn test                         # the original suite: 12 of 17 pass
```

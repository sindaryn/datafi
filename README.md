# Datafi  
  
Datafi auto-generates the data access layer for Spring-Data-Jpa applications.  
  
 - No more boilerplate JPaRepository interfaces.  
 - Custom Jpa resolvers with a few simple field-level annotations.  
 - Get all the features of Jpa for your entire data model, without writing a single line of data layer code.  
# Table of Contents  
+ [Installation](#installation)
+ [Requirements](#requirements)
+ [Hello World](#hello-world)
  - [Domain model](#domain-model)
  - [Service layer](#service-layer)
+ [StandardPersistableEntity](#standardpersistableentity)
  - [Domain model](#domain-model-1)
+ [IdFactory](#idfactory)
+ [Custom resolvers](#custom-resolvers)
  - [@GetBy, and @GetAllBy](#-getby--and--getallby)
    * [Domain model](#domain-model-2)
    * [Example Service Layer](#example-service-layer)
  - [@GetByUnique](#-getbyunique)
    * [Domain model](#domain-model-3)
    * [Example Service Layer](#example-service-layer-1)
  - [@WithResolver(...)](#-withresolver--)
    * [Domain model](#domain-model-4)
    * [Data access layer](#data-access-layer)
    * [Example Service Layer](#example-service-layer-2)
  - [Syntactic sugar](#syntactic-sugar)
+ [cascadedUpdate](#cascadedupdate)
    + [cascadeUpdateCollection](#cascadeupdatecollection)
    + [Excluding fields from cascadeUpdate(...) operations](#excluding-fields-from-cascadeupdate---operations)
+ [Mutating the state of foreign key Iterables](#mutating-the-state-of-foreign-key-iterables)
### Installation  
Datafi is available on maven central:  

```
<dependency>
    <groupId>org.sindaryn</groupId>
        <artifactId>datafi</artifactId>
    <version>0.0.1</version>
</dependency>
```

### Requirements
1. The main class must be annotated either with `@SpringBootApplication`, or `@MainClass`.
2. All entities **must** have a public `getId()` method.

### Hello World  
Datafi autogenerates Jpa repositories for all data model entities annotated with `@Entity` and / or `@Table` annotation(s).  
To make use of this, `@Autowire` the `DataManager<T>` bean into your code, as follows:  
  
#### Domain model  
```  
  
@Entity  
@PersistableEntity  
public class Person{  
     @Id 
     private String id = UUID.randomUUID().toString(); 
     private String name; 
     private Integer age; 
     // getters & setters, etc...
}  
```  
#### Service layer  
Now any `JpaRepository` or `JpaSpecificationExecutor` method can be called. For example: `findById(id)`  
```  
@Service  
public class PersonService{  

     @Autowired 
     private DataManager<Person> personDataManager;     
     
     public Person getPersonById(String id){  
     return personDataManager.findById(Person.class, id).orElse(...); 
     }
}  
```  
As you can see, the first argument to the data accessor method is `Person.class`. What's happening under the hood is that at startup, a hashmap is created to map entity types to their respective JpaRepositories. Even though `DataManager<T>` is a generic, at runtime it has no internal way of knowing which entity type it is serving. This is thanks to java type erasure. The bottom line is that for every method which does not naturally include an actual instance (or list of instances) of the given type, the appropriate class type token must be provided as the first argument. 

Alternately, you can manually call the `setType(Class<T> type)` method in `DataManger<T>`, and then omit the class type token as the first argument to subsequent method calls. With the above example, it might look as follows:  
```  
@Service  
public class PersonService{  

     @Autowired 
     private DataManager<Person> personDataManager;     
     
     @PostConstruct  
     private void init(){ 
        personDataManager.setType(Person.class); 
     }     
     
     public Person getPersonById(String id){  
        return personDataManager.findById(id).orElse(...); 
     }
}  
```  
  
### StandardPersistableEntity
If you don't wan't to worry about assigning `@Id` or `@Version` column. the `StandardPersistableEntity` `@MappedSuperclass` can be extended. For example:  
#### Domain model  
```  
@Entity  
public class Person extends StandardPersistableEntity {  
    private String name; 
    private Integer age; 
    // getters & setters, etc...
}  
```  
  
### IdFactory
Alternately, the unix timestamp based `Long IdFactory.getNextId()` static method can be employed. For example:  
  
```  
@Entity  
public class Person{  
     @Id 
     private Long id = IdFactory.getNextId(); 
     private String name; 
     private Integer age; 
     // getters & setters, etc...
}  
```  
  
### Custom resolvers  
  
#### @GetBy, and @GetAllBy  
In addition to the standard JpaRepository methods included by default, you can annotate any field with `@GetBy` and / or `@GetAllBy` annotation, and this will generate a corresponding `findBy...(value)`, or `findAllBy...In(List<...> values)`. For example:  
  
##### Domain model  
```  
@Entity
public class Person{  
     @Id 
     private String id = UUID.randomUUID().toString(); 
     @GetBy 
     private String name;
     @GetAllBy 
     private Integer age; 
     @GetBy @GetAllBy 
     private String address; 
     // getters & setters, etc..
}  
```  
As can be observed, a field can have both annotations at the same time.  
##### Example Service Layer  
```  
@Service  
public class PersonService{  

     @Autowired 
     private DataManager<Person> personDataManager; 
     
     /* corresponds to @GetBy private String name; 
        Returns a list of persons with the (same) given name */
     public List<Person> getPersonsByName(String name){ 
     return personDataManager.getBy(Person.class, "name", name).orElse(...); 
     }     
     
     //corresponds to @GetAllBy private Integer age; 
     public List<Person> getAllPersonsByAge(List<Integer> ages){ 
        return personDataManager.getAllBy(Person.class, "age", ages).orElse(...); 
         
     }     
     
     //the following two methods correspond to @GetBy @GetAllBy private String address;  
     public List<Person> getPersonsByAddress(String address){ 
        return personDataManager.getBy(Person.class, "address", address).orElse(...);    
     }  
     
     public List<Person> getAllPersonsByAddressIn(List<String> addresses){  
        return personDataManager.getAllBy(Person.class, "address", addresses).orElse(...);
     }
}  
```
#### @GetByUnique
As can be observed, the return type of both of the previous methods is a list. That's because there is no gaurantee of uniqueness with regards to a field simply because it's been annotated with `@GetBy` and/or `@GetAllBy`. This is where `@GetByUnique` differs; it takes a unique value argument, and returns a single corresponding entity. In order for this to be valid syntax, any field annotated with the `@GetByUnique` annotation must also be annotated with `@Column(unique = true)`. If a field is annotated with only `@GetByUnique` but not `@Column(unique = true)`, a compilation error will occur. The following is an illustrative example:
##### Domain model  
```  
@Entity
public class Person{  

     @Id 
     private String id = UUID.randomUUID().toString(); 
     
     @GetByUnique
     @Column(unique = true) 
     private String name;
    //...
}  
```  
##### Example Service Layer  
```  
@Service  
public class PersonService{  

     @Autowired 
     private DataManager<Person> personDataManager; 
     
     /* 
     corresponds to 
     @GetByUnique 
     private String name; 
     Returns a single person with the given name
     */ 
     public Person getPersonByUniqueName(String name){ 
        return personDataManager.getByUnique(Person.class, "name", name).orElse(...); 
     } 
}  

```
#### @WithResolver(...)  
Thus far, the range of functionality required for most standard data layer operations has been covered. However, there are use cases where a more complex approach is required. One way in which `JpaRepository` addresses this need with the `@Query` annotation, with which the developer can take full advantage of the full power of either JPQL, or whichever native dialect is in use (by setting `@Query(..., nativeQuery = true, ...)`). Datafi addresses this need as well, by enabling the developer to define their own custom query based *resolver* methods, using the `@WithResolver(...)` as a **repeatable** class level annotation. See the following example:
##### Domain model  
```  
@Entity
@WithResolver(name = "getByNameOrAddress", where = "p.name = :name OR p.address = :address", args = {"name", "address"})  
public class Person {  

     @Id 
     private String id = UUID.randomUUID().toString(); 
     
     private String name; 
     
     private Integer age; 
     
     private String address;
}  
```  
##### Data access layer  
After running the compiler, the following code is generated: 
```  
@Repository  
public interface PersonDao extends GenericDao<String, Person> {  
     @Query("SELECT p FROM Person p WHERE p.name = :name OR p.address = :address") 
     List<Person> getByNameOrAddress(@Param("name") String name, @Param("address") String address);
}  
```  
Important note: The SQL placeholder is always the first character of the entity name, in lowercase. In the above example, it's the lowercase letter "p". 
...  

##### Example Service Layer  
```  
@Service  
public class PersonService{  
     @Autowired 
     private DataManager<Person> personDataManager;    
     public List<Person> getPersonsByNameOrAddress(String name, String address){  
     return personDataManager.selectByResolver(Person.class, "getByNameOrAddress", name, address); 
     }
}  
```  
Breakdown:   
 - The first argument is the class type token for the currently given entity (in this case Person.class).  
 - The second argument is the name of the resolver to be called, this is the value that was inputted as `@WithResolver(..., name = "getByNameOrAddress", ...) `.   
 - The third *set* of arguments are of the __same type and order__ as specified within the annotation - i.e. `@WithResolver(..., args = {"name", "address"}, ...)`.  
  
  
#### Syntactic sugar  
The `@WithResolver(...)` annotation comes with the following useful defaults::  
1) if the `where` argument is left unsassigned, it defaults to placing an `AND` contional between all of the arguments provided for the `args` parameter. For example, for the following domain model:   
    ```  
    @Entity  
    @WithResolver(name = "getByIdAndNameAndAddress", args = {"id", name", "address"}) 
    public class Person { 
    @Id private String id = UUID.randomUUID().toString(); 
    private String name; 
    private Integer age; 
    private String address; 
    } 
    ```  
    The resulting data layer code would look as follows:  
    ```  
     @Repository 
     public interface PersonDao extends GenericDao<String, Person> { 
     @Query("SELECT p FROM Person p WHERE p.id = :id AND p.name = :name AND p.address = :address") 
     List<Person> getByIdAndNameAndAddress( @Param("id") String id, @Param("name") String name, @Param("address") String address); } 
     ```  
     This would also happen if `"&&&"` was explicitly assigned to the `where` argument.  

2) If `"|||"` is assigned to the `where` parameter, an `OR` conditional is then inserted in between all of the arguments, as is the case in the following example:  
  
    ```  
    @Entity  
    @WithResolver(name = "getByIdOrNameOrAddress", where = "|||", args = {"id", name", "address"}) 
    public class Person { 
        @Id 
        private String id = UUID.randomUUID().toString(); 
        private String name; 
        private Integer age; 
        private String address; 
    } 
     ```  
    The resulting autogenerated data layer code would then look as follows:  
    ```  
     @Repository 
     public interface PersonDao extends GenericDao<String, Person> { 
         @Query("SELECT p FROM Person p WHERE p.id = :id OR p.name = :name OR p.address = :address") 
         List<Person> getByIdOrNameOrAddress( @Param("id") String id, @Param("name") String name, @Param("address") String address); 
     } 
     ```  
     Just to clarify - the method name assigned to the `name` parameter is completely arbitrary. Just make sure to remember it for later use via `DataManager<T>`.
     
### cascadedUpdate  
One issue which requires attention when designing a data model is cascading. Datafi simplifes this by offering out-of-the-box, built in application layer cascading when applying update operations. See illustration:
```  
@Service  
public class PersonService{  

     @Autowired 
     private DataManager<Person> personDataManager;    
     
     public Person updatePerson(Person toUpdate, Person objectWithUpdatedValues){  
        return personDataManager.cascadedUpdate(toUpdate,  objectWithUpdatedValues);
     }
     
}  
```  
  
Breakdown:   
 - The first argument is the `Person` instance we wish to update.  
   
 - The second argument is an instance of `Person` containing the updated values to be assigned to the corresponding fields within the first `Person` instance.  All of the it's other fields **must** be null.   
   
    **Important note**: This method skips over any iterables.  
  
### cascadeUpdateCollection  
`cascadeUpdateCollection` offers analogous functionality as `cascadeUpdate`, in plural. For Example:
  
```  
@Service  
public class PersonService{  

     @Autowired 
     private DataManager<Person> personDataManager; 
     
     //obviously, these two lists must correspond in length 
     public List<Person> updatePersons(List<Person> toUpdate, List<Person> objectsWithUpdatedValues){ 
        return personDataManager.cascadeUpdateCollection(toUpdate, objectsWithUpdatedValues); 
     }
     
}  
```  
  
### Excluding fields from cascadeUpdate(...) operations  
Field(s) to be excluded from `cascadeUpdate` operations should be annotated as `@NonCascadeUpdatable`. Alternately, if there are many such fields in a class and the developer would rather avoid the field-level annotational clutter, the class itself can be annotated with `@NonCascadeUpdatables`, with the relevant field names passsed as arguments. For example, the following:
  
 ```     
 @Entity  
 public class Person { 
 
     @Id 
     private String id = UUID.randomUUID().toString();
     
     @NonCascadeUpdatable 
     private String name; 
     
     @NonCascadeUpdatable 
     private Integer age; 
     
     private String address; 
     
 }
 ```
 is equivalent to:  
  
 ```     
@Entity  
@NonCascadeUpdatables({"name", "age"}) 
public class Person { 

    @Id 
    private String id = UUID.randomUUID().toString(); 
    
    private String name; 
    
    private Integer age; 
    
    private String address; 
    
} 
```  
### Mutating the state of foreign key Iterables  
As metioned above, `cascadeUpdate` operations skip over iterable type fields. This is due to the fact that collection mutations involve adding or removing elements to or from the collection - not mutations on the collection container itself. Therefore, `DataManager<T>` includes the following methods to help with adding to, and removing from, foreign key collections.  
1. `public<HasTs> List<T> addNewToCollectionIn(HasTs toAddTo, String fieldName, List<T> toAdd)`  
  This method takes in three arguments, while making internal use of the application level `cascadedUpdate` above in order to propogate the relevant state changes:  
   - `HasTs toAddTo` - The entity containing the foriegn key collection of "Ts" (The type of entities referenced in the collection) to which to add.
   - `String fieldName` - The field name of the foreign key collection (i.e. for `private Set<Person> friends;`, it'd be `"friends"`). 
   - `List<T> toAdd` - The entities to add to the collection.  
2. `public<HasTs> List<T> attachExistingToCollectionIn(HasTs toAddTo, String fieldName, List<T> toAttach)` Similar to the previous method but for one crucial difference; it ensures the entities to be _attached_ (**not added** from scratch) are indeed **already present** within their respective table within the database.
  
  
#### That's all for now, happy coding!  
License  
----  
  
Apache 2.0
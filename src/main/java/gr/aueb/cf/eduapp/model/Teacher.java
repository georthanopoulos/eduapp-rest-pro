package gr.aueb.cf.eduapp.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor                                                  // Default constructor (with NO params) -> public Teacher() {} ---> OBLIGATORY for all JPA entities!!! Hibernate does need it internally (reflection) in order to create objects when loading data from the DB.
@Table(name = "teachers")
public class Teacher extends AbstractEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID uuid = UUID.randomUUID();             // It runs as soon as the object is created! (when a constructor is called e.g: new User(). It is an instance initializer. (it runs as a part of the creation process of the object! the UUID exists even before the "save" for DB storing.

    @Column(unique = true, nullable = false)
    private String vat;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    @Setter(AccessLevel.PACKAGE)                     // We don't care about the Getter as it is Entity not a collection.
    @OneToOne(cascade = CascadeType.PERSIST,  fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter(AccessLevel.PACKAGE)                // Connection with region. Be careful with the Setter - Reduced access imposed!
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)       // cascade -> teacher delete  ara kai to info delete.   . and kanv Simultaneously with the update or delete of the personal info must be updated or deleted too.
    @JoinColumn(name = "personal_info_id")                           // Not needed "mappedBy:" on the other side because it is a unidirectional relationship. Consequently, we always go from the Teacher to PersonalInfo and NOT the opposite. (In fact, we can do it doesn't make sense!)
    private PersonalInfo personalInfo;


    // Helper Methods
    public void addUser(User user) {
        this.user = user;
        user.setTeacher(this);
    }

    public void removeUser (User user) {
        this.user = null;
        user.setTeacher(null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Teacher teacher)) return false;
        return Objects.equals(getVat(), teacher.getVat());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getVat());
    }
}

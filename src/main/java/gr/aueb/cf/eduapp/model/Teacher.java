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
@NoArgsConstructor
@Table(name = "teachers")
public class Teacher extends AbstractEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID uuid = UUID.randomUUID();

    @Column(unique = true, nullable = false)
    private String vat;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    @Setter(AccessLevel.PACKAGE)            // den maw endiaferei o getter giati einai entity den einai collection
    @OneToOne(cascade = CascadeType.PERSIST,  fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter(AccessLevel.PACKAGE)                // Connection with region. be careful with the setter. reduced access imposed.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)       // cascade -> teacher delete  ara kai to info delete.   . and kanv Simultaneously with the update or delete of the personal info must be updated or deleted too.
    @JoinColumn(name = "personal_info_id")                           // not needed to be done mappedBy apo thn allh pleyra giati einai unidirectional kai panta pame apo to teacher sto personal info kai oxi anapoda! mporoyme na to kanoyme alla den exei nohma!
    private PersonalInfo personalInfo;


    // Helper Methods
    public void addUser(User user) {
        this.user = user;
        user.setTeacher(this);
    }

    public void removeUser (User user) {
        this.user = user;
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

package gr.aueb.cf.eduapp.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "regions")
public class Region {                                        // Not needed to extends AbstractEntity because we would need timestamp, something that would make difficult the insert process.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Getter(AccessLevel.PACKAGE)                                 // We have collection -> reduce getter accesslevel too.
    @Setter(AccessLevel.PRIVATE)
    @OneToMany(mappedBy = "region",  fetch = FetchType.LAZY)
    private Set<Teacher> teachers = new HashSet<>();            // The declaration is "SET" interface! It must be Interface!

    public Set<Teacher> getTeachers() {
        return Collections.unmodifiableSet(teachers);
    }


    // Helper Methods
    public void addTeacher(Teacher teacher) {
        teachers.add(teacher);
        teacher.setRegion(this);
    }

    public void removeTeacher(Teacher teacher) {
        teachers.remove(teacher);
        teacher.setRegion(null);
    }
}

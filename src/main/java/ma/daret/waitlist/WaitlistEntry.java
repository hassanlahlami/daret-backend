package ma.daret.waitlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.daret.common.domain.BaseEntity;

/** A pre-launch marketing waitlist signup (spec G7). Just an email + locale. */
@Entity
@Table(name = "waitlist")
@Getter
@Setter
@NoArgsConstructor
public class WaitlistEntry extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String locale;

    public WaitlistEntry(String email, String locale) {
        this.email = email;
        this.locale = locale;
    }
}

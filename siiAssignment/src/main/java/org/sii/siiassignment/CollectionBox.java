package org.sii.siiassignment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class CollectionBox {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fundraising_event_id")
    @ToString.Exclude
    private FundraisingEvent fundraisingEvent;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "collection_box_amounts", joinColumns = @JoinColumn(name = "collection_box_id"))
    @MapKeyColumn(name = "currency")
    @Enumerated(EnumType.STRING)
    @Column(name = "amount")
    private Map<Currency, BigDecimal> amounts = new HashMap<>();

    public boolean isEmpty() {
        if (amounts == null || amounts.isEmpty()) {
            return true;
        }

        return amounts.values().stream().allMatch(amount -> amount == null || amount.compareTo(BigDecimal.ZERO) == 0);
    }

    public boolean isAssigned() {
        return this.fundraisingEvent != null;
    }

    public void clearAmounts() {
        if (this.amounts != null) {
            this.amounts.clear();
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        CollectionBox that = (CollectionBox) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

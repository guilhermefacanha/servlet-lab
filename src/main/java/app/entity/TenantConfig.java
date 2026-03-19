package app.entity;

import app.db.EncryptedStringConverter;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity
public class TenantConfig implements Serializable {

    private static final long serialVersionUID = -2630797343946801200L;

    @Id
    @Column(unique = true, nullable = false)
    private String tenantId;

    @Column(unique = true, nullable = false)
    private String host;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String dbUrl;

    @Column(nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String dbUsername;

    @Column(nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String dbPassword;

}
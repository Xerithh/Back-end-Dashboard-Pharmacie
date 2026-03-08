package pharmacie.entity;

import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@ToString
public class Fournisseur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE) // la clé est auto-générée par la BD, On ne veut pas de "setter"
    private Integer id;

    @NonNull
    @Size(min = 1, max = 255)
    @Column(unique = true, length = 255)
    @NotBlank
    private String nom;

    @NonNull
    @Email
    @Column(unique = true, length = 255)
    @NotBlank
    private String email;

    @ToString.Exclude
    @ManyToMany
    @JoinTable(name = "fournisseur_categorie", joinColumns = @JoinColumn(name = "fournisseur_id"), inverseJoinColumns = @JoinColumn(name = "categorie_code"))
    @JsonIgnoreProperties("fournisseurs")
    private Set<Categorie> categories = new LinkedHashSet<>();

    /**
     * Ajoute une catégorie aux catégories fournies par ce fournisseur
     * 
     * @param categorie la catégorie à ajouter
     */
    public void ajouterCategorie(Categorie categorie) {
        this.categories.add(categorie);
        categorie.getFournisseurs().add(this);
    }

    /**
     * Retire une catégorie des catégories fournies par ce fournisseur
     * 
     * @param categorie la catégorie à retirer
     */
    public void retirerCategorie(Categorie categorie) {
        this.categories.remove(categorie);
        categorie.getFournisseurs().remove(this);
    }
}

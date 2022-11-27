package co.edu.iudigital.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import co.edu.iudigital.app.model.Caso;

@Repository // redundante
public interface ICasoRepository extends JpaRepository<Caso, Long>{
	
	@Query("UPDATE Caso c SET c.visible = ?1 WHERE c.id = ?2")
	public Boolean setVisible(Boolean visible, Long id);
}

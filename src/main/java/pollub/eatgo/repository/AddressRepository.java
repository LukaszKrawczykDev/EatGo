package pollub.eatgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pollub.eatgo.model.Address;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIdDesc(Long userId);
    Optional<Address> findByIdAndUserId(Long id, Long userId);
}

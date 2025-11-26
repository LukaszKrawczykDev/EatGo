package pollub.eatgo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import pollub.eatgo.dto.address.AddressCreateDto;
import pollub.eatgo.dto.address.AddressDto;
import pollub.eatgo.model.Address;
import pollub.eatgo.model.User;
import pollub.eatgo.repository.AddressRepository;
import pollub.eatgo.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(AddressService.class)
class AddressServiceIntegrationTest {

    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .role(User.Role.CLIENT)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testAddAddress_Integration() {
        // Given
        AddressCreateDto dto = new AddressCreateDto(
                "Warszawa",
                "Testowa 1",
                "00-001",
                "5"
        );

        // When
        AddressDto result = addressService.addAddress(testUser.getId(), dto);

        // Then
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Warszawa", result.city());
        assertEquals("Testowa 1", result.street());
        assertEquals("00-001", result.postalCode());
        assertEquals("5", result.apartmentNumber());

        // Verify in database
        Optional<Address> savedAddress = addressRepository.findById(result.id());
        assertTrue(savedAddress.isPresent());
        assertEquals(testUser.getId(), savedAddress.get().getUser().getId());
    }

    @Test
    void testListAddresses_Integration() {
        // Given
        AddressCreateDto dto1 = new AddressCreateDto("Warszawa", "Testowa 1", "00-001", "5");
        AddressCreateDto dto2 = new AddressCreateDto("Kraków", "Testowa 2", "30-001", "10");

        addressService.addAddress(testUser.getId(), dto1);
        addressService.addAddress(testUser.getId(), dto2);

        // When
        List<AddressDto> result = addressService.listAddresses(testUser.getId());

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        // Should be ordered by id desc (newest first)
        assertTrue(result.get(0).id() >= result.get(1).id());
    }

    @Test
    void testUpdateAddress_Integration() {
        // Given
        AddressCreateDto createDto = new AddressCreateDto("Warszawa", "Testowa 1", "00-001", "5");
        AddressDto created = addressService.addAddress(testUser.getId(), createDto);

        AddressCreateDto updateDto = new AddressCreateDto(
                "Kraków",
                "Nowa 2",
                "30-002",
                "15"
        );

        // When
        AddressDto updated = addressService.updateAddress(testUser.getId(), created.id(), updateDto);

        // Then
        assertNotNull(updated);
        assertEquals(created.id(), updated.id());
        assertEquals("Kraków", updated.city());
        assertEquals("Nowa 2", updated.street());
        assertEquals("30-002", updated.postalCode());
        assertEquals("15", updated.apartmentNumber());

        // Verify in database
        Optional<Address> addressInDb = addressRepository.findById(updated.id());
        assertTrue(addressInDb.isPresent());
        assertEquals("Kraków", addressInDb.get().getCity());
    }
}


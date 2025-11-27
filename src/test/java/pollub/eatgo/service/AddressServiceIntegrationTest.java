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

        AddressCreateDto dto = new AddressCreateDto(
                "Warszawa",
                "Testowa 1",
                "00-001",
                "5"
        );

        AddressDto result = addressService.addAddress(testUser.getId(), dto);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Warszawa", result.city());
        assertEquals("Testowa 1", result.street());
        assertEquals("00-001", result.postalCode());
        assertEquals("5", result.apartmentNumber());

        Optional<Address> savedAddress = addressRepository.findById(result.id());
        assertTrue(savedAddress.isPresent());
        assertEquals(testUser.getId(), savedAddress.get().getUser().getId());
    }

    @Test
    void testListAddresses_Integration() {
        AddressCreateDto dto1 = new AddressCreateDto("Warszawa", "Testowa 1", "00-001", "5");
        AddressCreateDto dto2 = new AddressCreateDto("Kraków", "Testowa 2", "30-001", "10");

        addressService.addAddress(testUser.getId(), dto1);
        addressService.addAddress(testUser.getId(), dto2);

        List<AddressDto> result = addressService.listAddresses(testUser.getId());

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).id() >= result.get(1).id());
    }

    @Test
    void testUpdateAddress_Integration() {
        AddressCreateDto createDto = new AddressCreateDto("Warszawa", "Testowa 1", "00-001", "5");
        AddressDto created = addressService.addAddress(testUser.getId(), createDto);

        AddressCreateDto updateDto = new AddressCreateDto(
                "Kraków",
                "Nowa 2",
                "30-002",
                "15"
        );

        AddressDto updated = addressService.updateAddress(testUser.getId(), created.id(), updateDto);

        assertNotNull(updated);
        assertEquals(created.id(), updated.id());
        assertEquals("Kraków", updated.city());
        assertEquals("Nowa 2", updated.street());
        assertEquals("30-002", updated.postalCode());
        assertEquals("15", updated.apartmentNumber());

        Optional<Address> addressInDb = addressRepository.findById(updated.id());
        assertTrue(addressInDb.isPresent());
        assertEquals("Kraków", addressInDb.get().getCity());
    }

    @Test
    void testDeleteAddress_Integration() {
        AddressCreateDto dto = new AddressCreateDto("Warszawa", "Testowa 1", "00-001", "5");
        AddressDto created = addressService.addAddress(testUser.getId(), dto);

        addressService.deleteAddress(testUser.getId(), created.id());

        assertFalse(addressRepository.findById(created.id()).isPresent());
    }

    @Test
    void testListAddresses_EmptyForUserWithoutAddresses() {
        List<AddressDto> result = addressService.listAddresses(testUser.getId());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateAddress_WrongUser_ShouldThrow() {
        AddressCreateDto dto = new AddressCreateDto("Warszawa", "Testowa 1", "00-001", "5");
        AddressDto created = addressService.addAddress(testUser.getId(), dto);

        User otherUser = User.builder()
                .email("other@example.com")
                .password("password456")
                .fullName("Other User")
                .role(User.Role.CLIENT)
                .build();
        otherUser = userRepository.save(otherUser);

        AddressCreateDto updateDto = new AddressCreateDto("Gdańsk", "Nowa 2", "80-002", "7");

        User finalOtherUser = otherUser;
        assertThrows(RuntimeException.class,
                () -> addressService.updateAddress(finalOtherUser.getId(), created.id(), updateDto));
    }
}


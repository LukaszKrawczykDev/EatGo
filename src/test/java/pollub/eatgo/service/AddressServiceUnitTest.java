package pollub.eatgo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pollub.eatgo.dto.address.AddressCreateDto;
import pollub.eatgo.dto.address.AddressDto;
import pollub.eatgo.model.Address;
import pollub.eatgo.model.User;
import pollub.eatgo.repository.AddressRepository;
import pollub.eatgo.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceUnitTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressService addressService;

    private User testUser;
    private Address testAddress;
    private AddressCreateDto addressCreateDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(User.Role.CLIENT)
                .build();

        testAddress = Address.builder()
                .id(1L)
                .city("Warszawa")
                .street("Testowa 1")
                .postalCode("00-001")
                .apartmentNumber("5")
                .user(testUser)
                .build();

        addressCreateDto = new AddressCreateDto(
                "Warszawa",
                "Testowa 1",
                "00-001",
                "5"
        );
    }

    @Test
    void testAddAddress_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(addressRepository.save(any(Address.class))).thenReturn(testAddress);

        AddressDto result = addressService.addAddress(1L, addressCreateDto);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Warszawa", result.city());
        assertEquals("Testowa 1", result.street());
        assertEquals("00-001", result.postalCode());
        assertEquals("5", result.apartmentNumber());

        verify(userRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    void testListAddresses_Success() {
        Address address1 = Address.builder()
                .id(1L)
                .city("Warszawa")
                .street("Testowa 1")
                .postalCode("00-001")
                .apartmentNumber("5")
                .user(testUser)
                .build();

        Address address2 = Address.builder()
                .id(2L)
                .city("Kraków")
                .street("Testowa 2")
                .postalCode("30-001")
                .apartmentNumber("10")
                .user(testUser)
                .build();

        when(addressRepository.findByUserIdOrderByIdDesc(1L))
                .thenReturn(Arrays.asList(address2, address1));

        List<AddressDto> result = addressService.listAddresses(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Kraków", result.get(0).city());
        assertEquals("Warszawa", result.get(1).city());
        verify(addressRepository, times(1)).findByUserIdOrderByIdDesc(1L);
    }

    @Test
    void testDeleteAddress_Success() {
        when(addressRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testAddress));
        doNothing().when(addressRepository).delete(testAddress);

        assertDoesNotThrow(() -> addressService.deleteAddress(1L, 1L));

        verify(addressRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(addressRepository, times(1)).delete(testAddress);
    }


    @Test
    void testAddAddress_UserNotFound_ShouldThrow() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> addressService.addAddress(1L, addressCreateDto));
        verify(userRepository, times(1)).findById(1L);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void testUpdateAddress_AddressNotFound_ShouldThrow() {
        when(addressRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        AddressCreateDto updateDto = new AddressCreateDto(
                "Gdańsk",
                "Nowa 10",
                "80-001",
                "2"
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> addressService.updateAddress(1L, 99L, updateDto));
        assertEquals("Adres nie znaleziony", ex.getMessage());
        verify(addressRepository, times(1)).findByIdAndUserId(99L, 1L);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void testDeleteAddress_AddressNotFound_ShouldThrow() {
        when(addressRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> addressService.deleteAddress(1L, 99L));
        assertEquals("Adres nie znaleziony", ex.getMessage());
        verify(addressRepository, times(1)).findByIdAndUserId(99L, 1L);
        verify(addressRepository, never()).delete(any(Address.class));
    }
}


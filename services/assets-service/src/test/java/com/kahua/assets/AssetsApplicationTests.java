package com.kahua.assets;

import com.kahua.assets.entity.Operation;
import com.kahua.assets.entity.Shift;
import com.kahua.assets.repository.OperationRepository;
import com.kahua.assets.service.ShiftService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AssetsApplicationTests {

	@Autowired
	private ShiftService shiftService;

	@Autowired
	private OperationRepository operationRepository;

	@Test
	void testActiveShiftsSearchReturnsCorrectOperationId() {
		// Find any operation from the database
		List<Operation> operations = operationRepository.findAll();
		assertFalse(operations.isEmpty(), "Expected database to have seeded operations");

		for (Operation operation : operations) {
			UUID opId = operation.getOperationId();
			// Search for shifts associated with this operation under activeOperation = true
			List<Shift> shifts = shiftService.searchShifts(
					null, opId, null, null, null, null, null, null, null, null, null, true);

			for (Shift shift : shifts) {
				assertNotNull(shift.getOperation(), "Shift operation should not be null");
				assertEquals(opId, shift.getOperation().getOperationId(),
						"Expected shift operation ID to match the queried operation ID");
			}
		}
	}

}

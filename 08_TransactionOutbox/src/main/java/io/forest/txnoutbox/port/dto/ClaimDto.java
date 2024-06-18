package io.forest.txnoutbox.port.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Data;

@Data
public class ClaimDto {

	UUID id;

	LocalDate submissionDate;

	String note;
}

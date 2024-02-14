package io.forest.hibernate.port.out;

import io.forest.hibernate.common.ResponseDto;
import io.forest.hibernate.port.dto.NotificationDto;

public interface NotifierAdapter {

	ResponseDto<NotificationDto> notify(NotificationDto dto);

}

insert into CUSTOMERS(CUSTOMER_ID, FIRSTNAME, LASTNAME) values ('c725f120-5e66-4613-a2f8-8f0e4fbccf46', 'Fred', 'Flinstone');
insert into CUSTOMERS(CUSTOMER_ID, FIRSTNAME, LASTNAME) values ('1f153dd8-dbf6-4e33-a30f-1beeb3ac13bd', 'Barney', 'Rubble');


insert into ADDRESSES(ADDRESS_ID, CUSTOMER_ID, STREET) values('40c0f3ed-fded-48fe-90ae-f1d6b00b8147', 'c725f120-5e66-4613-a2f8-8f0e4fbccf46', '1313 Cobblestone Way, Bedrock, 70777');
insert into ADDRESSES(ADDRESS_ID, CUSTOMER_ID, STREET) values('ea205292-100b-4664-b356-507965c51b93', '1f153dd8-dbf6-4e33-a30f-1beeb3ac13bd', '303 Cobblestone Way, Bedrock, 70777');

insert into CONSENTS(CONSENT_ID, CUSTOMER_ID, CONSENT_TYPE) values('d8f2e1d6-da75-4509-b9eb-e3a233011791', 'c725f120-5e66-4613-a2f8-8f0e4fbccf46', 0)
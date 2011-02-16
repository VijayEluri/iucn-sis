CREATE TABLE UTEndUse (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTEndUse_valueLookup (id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UTCaptiveHarvest (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTHarvestTrendComments (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTLocalLivelihood (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTNatlCommercialValue (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTIntlCommercialValue (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));

CREATE TABLE UTWildHarvestRecord (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTWildHarvestRecord_sourceLookup (id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UTWildHarvestRecord_formRemovedLookup (id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UTWildHarvestRecord_lifeStageRemovalLookup (id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UTWildHarvestRecord_genderRemovalLookup (id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UTWildHarvestRecord_relativeHarvestLookup (id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UTEndUseRecord (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTEndUseRecordSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTEndUseRecord_endUseLookup (id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UTEndUseRecord_scaleLookup (id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UTEndUseRecord_driverLookup (id integer auto_increment primary key, name varchar(255), label varchar(255));

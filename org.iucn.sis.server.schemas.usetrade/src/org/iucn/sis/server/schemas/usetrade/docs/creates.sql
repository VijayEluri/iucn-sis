CREATE TABLE UTEndUse (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTEndUse_valueLookup (id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UTCaptiveHarvest (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTCaptiveHarvest_valueLookup (id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UTHarvestTrendComments (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTLocalLivelihood (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UTCommercialValue (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));

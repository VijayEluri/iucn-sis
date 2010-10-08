CREATE TABLE AOO (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE AOOContinuingDecline (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE AOOContinuingDecline_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE AOOExtremeFluctuation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE AreaRestricted (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE AssessmentGeographicScope_geographicScopeLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE AssessmentGeographicScope (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE AvgAnnualFecundity (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE AvgReproductiveAge_unitsLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE AvgReproductiveAge (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE BiogeographicRealm_realmLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE BiogeographicRealm (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE BirthSize (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ConservationActionsLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE ConservationActions (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ConservationActionsSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ConservationActionsDocumentation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE CountryOccurrenceLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE CountryOccurrence_presenceLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE CountryOccurrence_originLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE CountryOccurrence_seasonalityLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE CountryOccurrence (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE CountryOccurrenceSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE CropWildRelative (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE DateLastSeen (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE DepthLower (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE DepthUpper (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE DepthZone_depthZoneLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE DepthZone (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE EOO (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE EOO_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE EOOContinuingDecline (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE EOOContinuingDecline_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE EOOExtremeFluctuation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE EcosystemServicesLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE EcosystemServices_importanceLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE EcosystemServices_rangeOfBenefitLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE EcosystemServices (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE EcosystemServicesSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE EcosystemServicesInsufficientInfo (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE EcosystemServicesProvidesNone (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE EggLaying (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ElevationLower (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ElevationUpper (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ExtinctionProbabilityGenerations3 (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ExtinctionProbabilityGenerations5 (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ExtinctionProbabilityYears100 (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE FAOOccurrenceLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE FAOOccurrence_presenceLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE FAOOccurrence_originLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE FAOOccurrence_seasonalityLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE FAOOccurrence (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE FAOOccurrenceSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE FemaleMaturityAge_unitsLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE FemaleMaturityAge (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE FemaleMaturitySize (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE FreeLivingLarvae (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE GeneralHabitatsLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE GeneralHabitats_suitabilityLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE GeneralHabitats_majorImportanceLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE GeneralHabitats (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE GeneralHabitatsSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE GenerationLength (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE GestationTime_unitsLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE GestationTime (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE HabitatContinuingDecline (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE HabitatContinuingDecline_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE HabitatDocumentation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE InPlaceResearch_recoveryPlanLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceResearch_monitoringSchemeLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceResearch (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE InPlaceSpeciesManagement_harvestPlanLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceSpeciesManagement_reintroducedLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceSpeciesManagement_exSituConservationLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceSpeciesManagement (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE InPlaceLandWaterProtection_sitesIdentifiedLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceLandWaterProtection_rangeLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceLandWaterProtection_inPALookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceLandWaterProtection_areaPlannedLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceLandWaterProtection_invasiveControlLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceLandWaterProtection (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE InPlaceEducation_subjectToProgramsLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceEducation_internationalLegislationLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceEducation_controlledLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE InPlaceEducation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE LakesLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE Lakes_presenceLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Lakes_originLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Lakes (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE LakesSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE LandCoverLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE LandCover_nullLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE LandCover (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE LandCoverSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE LargeMarineEcosystemsLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE LargeMarineEcosystems_presenceLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE LargeMarineEcosystems_originLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE LargeMarineEcosystems_seasonalityLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE LargeMarineEcosystems (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE LargeMarineEcosystemsSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE LiveBirth (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE Livelihoods_scaleLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Livelihoods_unitsLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Livelihoods_humanRelianceLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Livelihoods_genderAgeLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Livelihoods_socioEconLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Livelihoods_percentPopBenefitLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Livelihoods_percentConsumeLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Livelihoods_percentIncomeLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Livelihoods (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE LivelihoodsSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE LocationContinuingDecline (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE LocationContinuingDecline_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE LocationExtremeFluctuation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE LocationsNumber (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE Longevity_unitsLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Longevity (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE MaleMaturityAge_unitsLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE MaleMaturityAge (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE MaleMaturitySize (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE MapStatus_statusLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE MapStatus (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE MaxSize (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE MaxSubpopulationSize (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE MovementPatterns_patternLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE MovementPatterns (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE NaturalMortality (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE NonConsumptiveUse (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE NonConsumptiveUseDescription (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE NotUtilized (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE NoThreats (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE OldDEMPastDecline (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE OldDEMPeriodPastDecline (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE OldDEMFutureDecline (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE OldDEMPeriodFutureDecline (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE OtherPublication (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PVAFile (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE Parthenogenesis (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PlantGrowthFormsLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE PlantGrowthForms (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PlantGrowthFormsSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationContinuingDecline (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationContinuingDecline_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE PopulationDeclineGenerations1 (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationDeclineGenerations1_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE PopulationDeclineGenerations2 (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationDeclineGenerations2_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE PopulationDeclineGenerations3 (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationDeclineGenerations3_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE PopulationDocumentation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationExtremeFluctuation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationIncreaseRate (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionFuture (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionFuture_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE PopulationReductionFutureBasis_valueLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE PopulationReductionFutureBasis (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionFutureCeased (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionFutureReversible (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionFutureUnderstood (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionPast (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionPast_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE PopulationReductionPastBasis_valueLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE PopulationReductionPastBasis (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionPastCeased (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionPastReversible (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionPastUnderstood (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionPastandFuture (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionPastandFuture_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE PopulationReductionPastandFutureBasis_valueLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE PopulationReductionPastandFutureBasis (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionPastandFutureCeased (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionPastandFutureReversible (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationReductionPastandFutureUnderstood (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationSize (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE PopulationTrend_valueLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE PopulationTrend (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RangeDocumentation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListAssessmentAuthors (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListAssessmentDate (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListAssessors (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListCategory (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListCaveat (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListConsistencyCheck_progressLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE RedListConsistencyCheck_successStatusLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE RedListConsistencyCheck (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListContributors (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListCriteria_critVersionLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE RedListCriteria (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListCriteriaVersion_criteriaVersionLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE RedListCriteriaVersion (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListEvaluated_statusLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE RedListEvaluated (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListEvaluationDate (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListEvaluators (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListFuzzyResult (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListManualCategory (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListManualCriteria (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListNotes (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListPetition (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListPublication (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListRationale (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListReasonsForChange_typeLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE RedListReasonsForChange_timeframeLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE RedListReasonsForChange_changeReasonsLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE RedListReasonsForChange_catCritChangesLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE RedListReasonsForChange (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListSource (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RedListText (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RegionInformation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RegionExpertQuestions (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ReproduictivePeriodicity (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ResearchLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE Research (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ResearchSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RiversLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE Rivers_presenceLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Rivers_originLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Rivers (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE RiversSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE SevereFragmentation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE SubpopulationContinuingDecline (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE SubpopulationContinuingDecline_qualifierLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE SubpopulationExtremeFluctuation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE SubpopulationNumber (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE SubpopulationSingle (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE System_valueLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE System (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE TaxonomicNotes (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ThreatsLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE Threats_timingLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Threats_scopeLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE Threats_severityLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE StressesLookup (id integer auto_increment primary key, code varchar(63), parentID varchar(63), level integer, codeable tinyint, ref varchar(63), description varchar(255));
CREATE TABLE Threats (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ThreatsSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE StressesSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ThreatsDocumentation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE ThreatsUnknown (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE TrendInDomesticOfftake_valueLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE TrendInDomesticOfftake (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE TrendInWildOfftake_valueLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE TrendInWildOfftake (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UseTradeDetails_purposeLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UseTradeDetails_sourceLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UseTradeDetails_formRemovedLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UseTradeDetails_unitsLookup(id integer auto_increment primary key, name varchar(255), label varchar(255));
CREATE TABLE UseTradeDetails (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UseTradeSubfield (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UseTradeDocumentation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE UseTradeNoInformation (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));
CREATE TABLE WaterBreeding (id integer auto_increment primary key, name varchar(255), data_type varchar(255), number_allowed varchar(255));

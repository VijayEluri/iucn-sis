DROP VIEW IF EXISTS $schema.vw_reference CASCADE;
CREATE VIEW $schema.vw_reference AS
  SELECT $schema.vw_filter.taxonid, $schema.vw_filter.assessmentid, f.id as fieldid, f.name as fieldname, r.*
  FROM $schema.vw_filter
	JOIN public.field f ON f.assessmentid = $schema.vw_filter.assessmentid
	JOIN public.field_reference ON public.field_reference.fieldid = f.id 
	JOIN public.reference r ON r.id = public.field_reference.referenceid;
GRANT SELECT ON $schema.vw_reference TO $user;

DROP VIEW IF EXISTS $schema.vw_reference_global CASCADE;
CREATE VIEW $schema.vw_reference_global AS 
  SELECT filter.taxonid, filter.assessmentid, r.*
    FROM $schema.vw_filter filter
    JOIN public.assessment_reference ar ON ar.assessmentid = filter.assessmentid
    JOIN reference r ON r.id = ar.referenceid;
GRANT SELECT ON $schema.vw_reference_global TO $user;

DROP VIEW IF EXISTS lookups."REGIONINFORMATION_REGIONSLOOKUP" CASCADE;
CREATE VIEW lookups."REGIONINFORMATION_REGIONSLOOKUP" AS 
  SELECT public.region.id AS "ID", CAST (public.region.id AS varchar(255)) AS "NAME", 
  public.region.name AS "LABEL" FROM public.region;
GRANT SELECT ON lookups."REGIONINFORMATION_REGIONSLOOKUP" TO PUBLIC;

DROP VIEW IF EXISTS lookups."REDLISTASSESSORS_VALUELOOKUP" CASCADE;
CREATE VIEW lookups."REDLISTASSESSORS_VALUELOOKUP" AS 
  SELECT public."user".id AS "ID", CAST (public."user".id AS varchar(255)) AS "NAME", 
  public."user".username AS "LABEL" FROM public."user";
GRANT SELECT ON lookups."REDLISTASSESSORS_VALUELOOKUP" TO PUBLIC;

DROP VIEW IF EXISTS lookups."REDLISTEVALUATORS_VALUELOOKUP" CASCADE;
CREATE VIEW lookups."REDLISTEVALUATORS_VALUELOOKUP" AS 
  SELECT public."user".id AS "ID", CAST (public."user".id AS varchar(255)) AS "NAME", 
  public."user".username AS "LABEL" FROM public."user";
GRANT SELECT ON lookups."REDLISTEVALUATORS_VALUELOOKUP" TO PUBLIC;

DROP VIEW IF EXISTS lookups."REDLISTCONTRIBUTORS_VALUELOOKUP" CASCADE;
CREATE VIEW lookups."REDLISTCONTRIBUTORS_VALUELOOKUP" AS 
  SELECT public."user".id AS "ID", CAST (public."user".id AS varchar(255)) AS "NAME", 
  public."user".username AS "LABEL" FROM public."user";
GRANT SELECT ON lookups."REDLISTCONTRIBUTORS_VALUELOOKUP" TO PUBLIC;

DROP VIEW IF EXISTS lookups."REDLISTFACILITATORS_VALUELOOKUP" CASCADE;
CREATE VIEW lookups."REDLISTFACILITATORS_VALUELOOKUP" AS 
  SELECT public."user".id AS "ID", CAST (public."user".id AS varchar(255)) AS "NAME", 
  public."user".username AS "LABEL" FROM public."user";
GRANT SELECT ON lookups."REDLISTFACILITATORS_VALUELOOKUP" TO PUBLIC;

DROP VIEW IF EXISTS lookups."THREATS_VIRUSLOOKUP" CASCADE;
CREATE VIEW lookups."THREATS_VIRUSLOOKUP" AS 
  SELECT public.virus.id AS "ID", CAST(public.virus.id AS varchar(255)) AS "NAME", 
  public.virus.name AS "LABEL" FROM public.virus;
GRANT SELECT ON lookups."THREATS_VIRUSLOOKUP" TO PUBLIC;

DROP VIEW IF EXISTS lookups."THREATS_IASLOOKUP" CASCADE;
CREATE VIEW lookups."THREATS_IASLOOKUP" AS 
  SELECT public.taxon.id AS "ID", public.taxon.name AS "NAME", 
  		CASE
  			WHEN taxon_levelid < 7 THEN 'Unspecified '||public.taxon.friendly_name
  			ELSE public.taxon.friendly_name
  		END AS "LABEL"
  FROM public.taxon 
  WHERE taxon.state = 0;
GRANT SELECT ON lookups."THREATS_IASLOOKUP" TO PUBLIC;

DROP VIEW IF EXISTS $schema.vw_redlistcategoryandcriteria CASCADE;
CREATE VIEW $schema.vw_redlistcategoryandcriteria AS
 SELECT vw.taxonid, vw.assessmentid,
        CASE
            WHEN s1.ismanual = true THEN s1.manualcategory
            ELSE s1.autocategory
        END AS rlcategory,
        CASE
            WHEN s1.ismanual = true THEN s1.manualcriteria
            ELSE s1.autocriteria
        END AS rlcriteria, s1.critversion, s1.ismanual
   FROM $schema.vw_filter vw
   LEFT JOIN ( 
      SELECT v.assessmentid, ff1.value AS autocategory, ff2.value AS autocriteria, 
        ff4.value AS critversion, ff7.value AS ismanual, ff8.value AS manualcategory, 
        ff9.value AS manualcriteria
        FROM $schema.vw_filter v
      JOIN public.field ON public.field.assessmentid = v.assessmentid AND public.field.name::text = 'RedListCriteria'::text
   LEFT JOIN public.primitive_field pf1 ON pf1.fieldid = public.field.id AND pf1.name::text = 'autoCategory'::text
   LEFT JOIN public.string_primitive_field ff1 ON ff1.id = pf1.id
   LEFT JOIN public.primitive_field pf2 ON pf2.fieldid = public.field.id AND pf2.name::text = 'autoCriteria'::text
   LEFT JOIN public.string_primitive_field ff2 ON ff2.id = pf2.id
   LEFT JOIN public.primitive_field pf4 ON pf4.fieldid = public.field.id AND pf4.name::text = 'critVersion'::text
   LEFT JOIN public.foreign_key_primitive_field ff4 ON ff4.id = pf4.id
   LEFT JOIN public.primitive_field pf7 ON pf7.fieldid = public.field.id AND pf7.name::text = 'isManual'::text
   LEFT JOIN public.boolean_primitive_field ff7 ON ff7.id = pf7.id
   LEFT JOIN public.primitive_field pf8 ON pf8.fieldid = public.field.id AND pf8.name::text = 'manualCategory'::text
   LEFT JOIN public.string_primitive_field ff8 ON ff8.id = pf8.id
   LEFT JOIN public.primitive_field pf9 ON pf9.fieldid = public.field.id AND pf9.name::text = 'manualCriteria'::text
   LEFT JOIN public.string_primitive_field ff9 ON ff9.id = pf9.id) s1 ON vw.assessmentid = s1.assessmentid;
GRANT SELECT ON $schema.vw_redlistcategoryandcriteria TO $user;

DROP VIEW IF EXISTS $schema.vw_redlistcaveat_value CASCADE;
CREATE VIEW $schema.vw_redlistcaveat_value AS
 SELECT vw.taxonid, vw.assessmentid, CASE WHEN vw.value IS NULL OR vw.value > '$caveat' THEN 'false' ELSE 'true' END as value
 FROM $schema.vw_redlistassessmentdate_value vw;
GRANT SELECT ON $schema.vw_redlistcaveat_value TO $user;

DROP VIEW IF EXISTS $schema.vw_redlistassessors_publication CASCADE;
CREATE VIEW $schema.vw_redlistassessors_publication AS 
 SELECT v.taxonid, v.assessmentid, array_to_string(array_agg(v.value), ', ') as value
 FROM (
  SELECT DISTINCT vw.taxonid, vw.assessmentid, 
   CASE
    WHEN vw.text IS NULL THEN
     CASE
      WHEN u.initials = '' AND u.first_name = '' THEN u.last_name
      WHEN u.initials = '' THEN u.last_name||', '||substring(u.first_name from 1 for 1)||'.'
      ELSE u.last_name||', '||u.initials
     END
    ELSE vw.text
    END AS value
   FROM ( 
    SELECT vw_filter.taxonid, vw_filter.assessmentid, s1.text, s1.value
    FROM $schema.vw_filter
    LEFT JOIN ( SELECT vw_filter.assessmentid, ff1.value AS text, ff2.value
            FROM $schema.vw_filter
       JOIN field ON field.assessmentid = vw_filter.assessmentid AND field.name::text = 'RedListAssessors'::text
    LEFT JOIN primitive_field pf1 ON pf1.fieldid = field.id AND pf1.name::text = 'text'::text
    LEFT JOIN string_primitive_field ff1 ON ff1.id = pf1.id
    LEFT JOIN primitive_field pf2 ON pf2.fieldid = field.id AND pf2.name::text = 'value'::text
    LEFT JOIN foreign_key_list_primitive_field fi2 ON fi2.id = pf2.id
    LEFT JOIN fk_list_primitive_values ff2 ON ff2.fk_list_primitive_id = pf2.id) s1 ON vw_filter.assessmentid = s1.assessmentid
  ) vw
  LEFT JOIN public."user" u ON u.id = vw.value
  ORDER BY taxonid, value
 ) v
 GROUP BY taxonid, assessmentid;
GRANT SELECT ON $schema.vw_redlistassessors_publication TO $user;

DROP VIEW IF EXISTS $schema.vw_redlistevaluators_publication CASCADE;
CREATE VIEW $schema.vw_redlistevaluators_publication AS 
 SELECT v.taxonid, v.assessmentid, array_to_string(array_agg(v.value), ', ') as value
 FROM (
  SELECT DISTINCT vw.taxonid, vw.assessmentid, 
   CASE
    WHEN vw.text IS NULL THEN
     CASE
      WHEN u.initials = '' AND u.first_name = '' THEN u.last_name
      WHEN u.initials = '' THEN u.last_name||', '||substring(u.first_name from 1 for 1)||'.'
      ELSE u.last_name||', '||u.initials
     END
    ELSE vw.text
    END AS value
   FROM ( 
    SELECT vw_filter.taxonid, vw_filter.assessmentid, s1.text, s1.value
    FROM $schema.vw_filter
    LEFT JOIN ( SELECT vw_filter.assessmentid, ff1.value AS text, ff2.value
            FROM $schema.vw_filter
       JOIN field ON field.assessmentid = vw_filter.assessmentid AND field.name::text = 'RedListEvaluators'::text
    LEFT JOIN primitive_field pf1 ON pf1.fieldid = field.id AND pf1.name::text = 'text'::text
    LEFT JOIN string_primitive_field ff1 ON ff1.id = pf1.id
    LEFT JOIN primitive_field pf2 ON pf2.fieldid = field.id AND pf2.name::text = 'value'::text
    LEFT JOIN foreign_key_list_primitive_field fi2 ON fi2.id = pf2.id
    LEFT JOIN fk_list_primitive_values ff2 ON ff2.fk_list_primitive_id = pf2.id) s1 ON vw_filter.assessmentid = s1.assessmentid
  ) vw
  LEFT JOIN public."user" u ON u.id = vw.value
  ORDER BY taxonid, value
 ) v
 GROUP BY taxonid, assessmentid;
GRANT SELECT ON $schema.vw_redlistevaluators_publication TO $user;

DROP VIEW IF EXISTS $schema.vw_redlistcontributors_publication CASCADE;
CREATE VIEW $schema.vw_redlistcontributors_publication AS 
 SELECT v.taxonid, v.assessmentid, array_to_string(array_agg(v.value), ', ') as value
 FROM (
  SELECT DISTINCT vw.taxonid, vw.assessmentid, 
   CASE
    WHEN vw.text IS NULL THEN
     CASE
      WHEN u.initials = '' AND u.first_name = '' THEN u.last_name
      WHEN u.initials = '' THEN u.last_name||', '||substring(u.first_name from 1 for 1)||'.'
      ELSE u.last_name||', '||u.initials
     END
    ELSE vw.text
    END AS value
   FROM ( 
    SELECT vw_filter.taxonid, vw_filter.assessmentid, s1.text, s1.value
    FROM $schema.vw_filter
    LEFT JOIN ( SELECT vw_filter.assessmentid, ff1.value AS text, ff2.value
            FROM $schema.vw_filter
       JOIN field ON field.assessmentid = vw_filter.assessmentid AND field.name::text = 'RedListContributors'::text
    LEFT JOIN primitive_field pf1 ON pf1.fieldid = field.id AND pf1.name::text = 'text'::text
    LEFT JOIN string_primitive_field ff1 ON ff1.id = pf1.id
    LEFT JOIN primitive_field pf2 ON pf2.fieldid = field.id AND pf2.name::text = 'value'::text
    LEFT JOIN foreign_key_list_primitive_field fi2 ON fi2.id = pf2.id
    LEFT JOIN fk_list_primitive_values ff2 ON ff2.fk_list_primitive_id = pf2.id) s1 ON vw_filter.assessmentid = s1.assessmentid
  ) vw
  LEFT JOIN public."user" u ON u.id = vw.value
  ORDER BY taxonid, value
 ) v
 GROUP BY taxonid, assessmentid;
GRANT SELECT ON $schema.vw_redlistcontributors_publication TO $user;

DROP VIEW IF EXISTS $schema.vw_redlistfacilitators_publication CASCADE;
CREATE VIEW $schema.vw_redlistfacilitators_publication AS 
 SELECT v.taxonid, v.assessmentid, array_to_string(array_agg(v.value), ', ') as value
 FROM (
  SELECT DISTINCT vw.taxonid, vw.assessmentid, 
   CASE
    WHEN vw.text IS NULL THEN
     CASE
      WHEN u.initials = '' AND u.first_name = '' THEN u.last_name
      WHEN u.initials = '' THEN u.last_name||', '||substring(u.first_name from 1 for 1)||'.'
      ELSE u.last_name||', '||u.initials
     END
    ELSE vw.text
    END AS value
   FROM ( 
    SELECT vw_filter.taxonid, vw_filter.assessmentid, s1.text, s1.value
    FROM $schema.vw_filter
    LEFT JOIN ( SELECT vw_filter.assessmentid, ff1.value AS text, ff2.value
            FROM $schema.vw_filter
       JOIN field ON field.assessmentid = vw_filter.assessmentid AND field.name::text = 'RedListFacilitators'::text
    LEFT JOIN primitive_field pf1 ON pf1.fieldid = field.id AND pf1.name::text = 'text'::text
    LEFT JOIN string_primitive_field ff1 ON ff1.id = pf1.id
    LEFT JOIN primitive_field pf2 ON pf2.fieldid = field.id AND pf2.name::text = 'value'::text
    LEFT JOIN foreign_key_list_primitive_field fi2 ON fi2.id = pf2.id
    LEFT JOIN fk_list_primitive_values ff2 ON ff2.fk_list_primitive_id = pf2.id) s1 ON vw_filter.assessmentid = s1.assessmentid
  ) vw
  LEFT JOIN public."user" u ON u.id = vw.value
  ORDER BY taxonid, value
 ) v
 GROUP BY taxonid, assessmentid;
GRANT SELECT ON $schema.vw_redlistfacilitators_publication TO $user;

DROP VIEW IF EXISTS $schema.vw_stressessubfield_stress CASCADE;
CREATE OR REPLACE VIEW $schema.vw_stressessubfield_stress AS 
 SELECT vw_filter.taxonid, vw_filter.assessmentid, thr.id AS recordid, ff.value
   FROM $schema.vw_filter
   JOIN field ON field.assessmentid = vw_filter.assessmentid AND field.name::text = 'Threats'::text
   JOIN field thr ON thr.parentid = field.id AND thr.name::text = 'ThreatsSubfield'::text
   JOIN field sf ON sf.parentid = thr.id
   JOIN primitive_field pf ON pf.fieldid = sf.id
   JOIN foreign_key_primitive_field ff ON ff.id = pf.id
  WHERE sf.name::text = 'StressesSubfield'::text AND pf.name::text = 'stress'::text;
GRANT SELECT ON $schema.vw_stressessubfield_stress TO $user;

DROP VIEW IF EXISTS $schema.vw_workingsettaxon CASCADE;
CREATE VIEW $schema.vw_workingsettaxon AS
  SELECT DISTINCT vwf.taxonid, ws.name
  FROM $schema.vw_filter vwf
  JOIN public.working_set_taxon wst ON vwf.taxonid = wst.taxonid
  JOIN public.working_set ws ON ws.id = wst.working_setid;
GRANT SELECT ON $schema.vw_workingsettaxon TO $user;

DROP VIEW IF EXISTS $schema.vw_common_name;
CREATE VIEW $schema.vw_common_name AS
  SELECT taxonid, name
  FROM common_name
  WHERE principal = true
  GROUP BY name, taxonid;
GRANT SELECT ON $schema.vw_common_name TO $user;

DROP VIEW IF EXISTS $schema.vw_synonym;
CREATE VIEW $schema.vw_synonym AS
  SELECT taxonid, friendly_name
  FROM synonym;
GRANT SELECT ON $schema.vw_synonym TO $user;

DROP VIEW IF EXISTS $schema.vw_common_name_all;
CREATE VIEW $schema.vw_common_name_all AS
  SELECT taxonid, name
  FROM common_name
  WHERE principal = false;
GRANT SELECT ON $schema.vw_common_name_all TO $user; 

DROP VIEW IF EXISTS $schema.vw_threatssubfield_virus_publication;
CREATE VIEW $schema.vw_threatssubfield_virus_publication AS 
  SELECT v.taxonid, v.assessmentid, v.recordid, virus.name as value
  FROM $schema.vw_threatssubfield_virus v
  LEFT JOIN virus ON virus.id = v.value;
GRANT SELECT ON $schema.vw_threatssubfield_virus_publication TO $user;

DROP VIEW IF EXISTS $schema.vw_threatssubfield_ias_publication CASCADE;
CREATE VIEW $schema.vw_threatssubfield_ias_publication AS 
  SELECT l.taxonid, l.assessmentid, l.recordid, t."LABEL"
  FROM $schema.vw_threatssubfield_threatslookup l
  JOIN lookups."THREATSLOOKUP" code ON code."ID" = l.value AND l.value IN (82, 85)
  JOIN $schema.vw_threatssubfield_ias i ON l.recordid = i.recordid
  LEFT JOIN lookups."THREATS_IASLOOKUP" t ON i.value = t."ID";
GRANT SELECT ON $schema.vw_threatssubfield_ias_publication TO $user;

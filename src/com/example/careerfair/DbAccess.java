/** 
 * This class handles all database access needs with methods that query the database and return the results in a structed format
 * (ex. ArrayList)
 * 
 * @author Hannah Wilder (with code borrowed from http://blog.softeq.com/2012/12/using-pre-populated-sqlite-database-in.html)
 * @version 1.1
 */

package com.example.careerfair;

import java.util.ArrayList;
import java.util.HashMap;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.ListView;

public class DbAccess {

	private static final String DB_NAME = "careerFairDB.db";

	//A good practice is to define database field names as constants
	private static final String COMPANY_TABLE_NAME = "company";
	private static final String COMPANY_ID = "_id";
	private static final String COMPANY_NAME = "name";


	/**
	 * Queries the database to obtain a list of company names and fill an array list with them
	 * 
	 * @param companies - an empty array list to fill with company names
	 * @param database - the database to query (obtain database with ExternalDbOpenHelper instance)
	 */
	public static void fillCompanies(ArrayList companies, SQLiteDatabase database) {
		//companies = new ArrayList<String>();
		Cursor companiesCursor = database.query(COMPANY_TABLE_NAME, new String[] {COMPANY_ID,
				COMPANY_NAME}, null, null, null, null, COMPANY_NAME);
		companiesCursor.moveToFirst();
		if(!companiesCursor.isAfterLast()) {
			do {
				String name = companiesCursor.getString(1);
				companies.add(name);
			} while (companiesCursor.moveToNext());
		}
		companiesCursor.close();
	}
	
	/**
     * getAllCompanies - gets all the companies in the database
     * 
     * @param companies - an ArrayList to fill with companies
     * @param database - SQLite database object returned by ExternalDbOpenHelper.openDataBase
     */
	public static void getAllCompanies(ArrayList<Company> companies, SQLiteDatabase database) {
		Cursor companiesCursor = database.rawQuery("SELECT DISTINCT company.name, company.website, location.tableNum, room.name FROM company, companyToLocation, location, room WHERE company._id=companyToLocation.companyID AND companyToLocation.locationID=location._id AND location.roomID=room._id ORDER BY company.name;", new String[0]);
		companiesCursor.moveToFirst();
		if(!companiesCursor.isAfterLast()) {
			do {
				String name = companiesCursor.getString(0);
				String website = companiesCursor.getString(1);
				String tableNum = companiesCursor.getString(2);
				String room = companiesCursor.getString(3);
				
				Company newCompany = new Company(name, website, tableNum, room, getMajorsForCompany(name, database), getPositionsForCompany(name, database), getWorkAuthsForCompany(name, database));
				companies.add(newCompany);
				
				
			} while (companiesCursor.moveToNext());
		}
		companiesCursor.close();
	}
	
	/**
     * getAllCompanies - gets all the companies in the database
     * 
     * @param database - SQLite database object returned by ExternalDbOpenHelper.openDataBase
     * @return an ArrayList filled with companies
     */
	public static ArrayList<Company> getAllCompanies(SQLiteDatabase database) {
		ArrayList<Company> companies = new ArrayList<Company>();
		getAllCompanies(companies, database);
		return companies;
	}
	
	/**
     * getCompaniesWith - gets all the companies in the database fitting a specific set of criteria
     * 
     * @param filterName - a string that the returned company names should contain, if not used, enter empty string ""
     * @param filterMajor - an ArrayList of majors to filter by, this filter will find all companies with at least one of the specified majors
     * @param filterWorkAuth - an ArrayList of strings to filter work authorizations by, this filter will find all the companies with at least one of the specified work authorizations
     * @param filterPosition - an ArrayList of strings to filter position types by, this filter will find all the companies with at least one of the specified position types
     * @param database - SQLite database object returned by ExternalDbOpenHelper.openDataBase
     */
	public static ArrayList<Company> getCompaniesWith(String filterName, ArrayList<Major> filterMajor, ArrayList<String> filterWorkAuth, ArrayList<String> filterPosition, SQLiteDatabase database) {
		ArrayList<Company> companies = new ArrayList<Company>();
		
		//Set default from and where strings (if there are no filters specified)
		String fromString = "FROM company, companyToLocation, location, room";
		String whereString = "WHERE company._id=companyToLocation.companyID AND companyToLocation.locationID=location._id AND location.roomID=room._id";
		
		//Handle Major filter
		if (!filterMajor.isEmpty()) {
			String majorSet = "'ALL'";
			for (Major major : filterMajor) {
				
				majorSet = majorSet + ", '" + major.getAbbrev() + "'";
			}
			whereString = whereString + " AND company._id=companyToMajor.companyID AND major._id=companyToMajor.majorID AND major.abbreviation IN (" + majorSet + ")";
			fromString = fromString + ", companyToMajor, major";
		}
		
		//Handle workAuth filter
		if(!filterWorkAuth.isEmpty()) {
			String workAuthSet = "'',";
			for (String workAuth : filterWorkAuth) {
				workAuthSet = workAuthSet + ", '" + workAuth + "'";
			}
			whereString = whereString + " AND company._id=companyToWorkAuth.companyID AND workAuth._id=companyToWorkAuth.workAuthID AND workAuth.type IN (" + workAuthSet + ")";
			fromString = fromString + ", companyToWorkAuth, workAuth";
		}
		
		//Handle position filter
		if(!filterPosition.isEmpty()) {
			String positionSet = "'',";
			for (String position : filterPosition) {
				positionSet = positionSet + ", '" + position + "'";
			}
			whereString = whereString + " AND company._id=companyToType.companyID AND employmentType._id=companyToType.typeID AND employmentType.type IN (" + positionSet + ")";
			fromString = fromString + ", companyToType, employmentType";
		}		
		
		//Handle name filter
		if (!filterName.isEmpty()) {
			filterName = "%" + filterName + "%";
			whereString = whereString + " AND company.name LIKE '" + filterName + "'";
		}
		
		//Toss strings into array to be passed into query 
		String[] query = {fromString, whereString};
		String wholeQuery = "SELECT DISTINCT company.name, company.website, location.tableNum, room.name " + fromString + " " + whereString + " ORDER BY company.name;";
		wholeQuery = wholeQuery + "";
		Cursor companiesCursor = database.rawQuery(wholeQuery, new String[0]);
		companiesCursor.moveToFirst();
		if(!companiesCursor.isAfterLast()) {
			do {
				String name = companiesCursor.getString(0);
				String website = companiesCursor.getString(1);
				String tableNum = companiesCursor.getString(2);
				String room = companiesCursor.getString(3);
				
				Company newCompany = new Company(name, website, tableNum, room, getMajorsForCompany(name, database), getPositionsForCompany(name, database), getWorkAuthsForCompany(name, database));
				companies.add(newCompany);
				
				
			} while (companiesCursor.moveToNext());
		}
		companiesCursor.close();
		
		return companies;
	}
	
	/**
     * getMajorsForCompany - gets all the majors a specific company is looking for
     * 
     * @param company - the name of the company to get the majors for
     * @param database - SQLite database object returned by ExternalDbOpenHelper.openDataBase
     * @return an ArrayList filled with majors
     */
	public static ArrayList<Major> getMajorsForCompany(String company, SQLiteDatabase database) {
		String[] nameArray = {company};
		
		ArrayList<Major> majors = new ArrayList<Major>();
		Cursor majorsCursor = database.rawQuery("SELECT major.name, major.abbreviation FROM company, companyToMajor, major WHERE company._id=companyToMajor.companyID AND company.name=? AND companyToMajor.majorID=major._id ORDER BY major.abbreviation;", nameArray);
		majorsCursor.moveToFirst();
		if(!majorsCursor.isAfterLast()) {
			do{
				String majorName = majorsCursor.getString(0);
				String majorAbbrev = majorsCursor.getString(1);
				majors.add(new Major(majorName, majorAbbrev));
			} while (majorsCursor.moveToNext());
		}
		majorsCursor.close();
		
		return majors;
	}
	
	/**
     * getPositionsForCompany - gets all the positions a specific company is hiring for
     * 
     * @param company - the name of the company to get the positions for
     * @param database - SQLite database object returned by ExternalDbOpenHelper.openDataBase
     * @return an ArrayList filled with positions
     */
	public static ArrayList<String> getPositionsForCompany(String company, SQLiteDatabase database) {
		String[] nameArray = {company};
		
		ArrayList<String> positions = new ArrayList<String>();
		Cursor positionsCursor = database.rawQuery("SELECT employmentType.type FROM company, companyToType, employmentType WHERE company._id=companyToType.companyID AND company.name=? AND companyToType.typeID=employmentType._id AND type<>'';", nameArray);
		positionsCursor.moveToFirst();
		if(!positionsCursor.isAfterLast()) {
			do{
				positions.add(positionsCursor.getString(0));
			} while (positionsCursor.moveToNext());
		}
		positionsCursor.close();
		
		return positions;
	}
	
	/**
     * getWorkAuthsForCompany - gets all the work authorization types a specific company is looking for
     * 
     * @param company - the name of the company to get the work authorizations for
     * @param database - SQLite database object returned by ExternalDbOpenHelper.openDataBase
     * @return an ArrayList filled with work authorizations
     */
	public static ArrayList<String> getWorkAuthsForCompany(String company, SQLiteDatabase database) {
		String[] nameArray = {company};
		
		ArrayList<String> workAuths = new ArrayList<String>();
		Cursor workAuthsCursor = database.rawQuery("SELECT workAuth.type FROM company, companyToWorkAuth, workAuth WHERE company._id=companyToWorkAuth.companyID AND company.name=? AND companyToWorkAuth.workAuthID=workAuth._id AND type<>'';", nameArray);
		workAuthsCursor.moveToFirst();
		if(!workAuthsCursor.isAfterLast()) {
			do {
				workAuths.add(workAuthsCursor.getString(0));
			} while (workAuthsCursor.moveToNext());
		}
		workAuthsCursor.close();
		
		return workAuths;
	}
	
	/**
     * getAllMajors - gets a list of all the majors
     * 
     * @param database - SQLite database object returned by ExternalDbOpenHelper.openDataBase
     * @param orderByName - orders by name if true, else ordered by abbreviation
     * @return an ArrayList filled with major objects
     */
	public static ArrayList<Major> getAllMajors(SQLiteDatabase database, boolean orderByName) {
		String[] empty ={};		
		
		ArrayList<Major> majors = new ArrayList<Major>();
		Cursor majorsCursor;
		if (orderByName) {
			majorsCursor = database.rawQuery("SELECT name, abbreviation FROM major ORDER BY name;", empty);
		} else {
			majorsCursor = database.rawQuery("SELECT name, abbreviation FROM major ORDER BY abbreviation;", empty);
		}
		
		majorsCursor.moveToFirst();
		if(!majorsCursor.isAfterLast()) {
			do{
				String majorName = majorsCursor.getString(0);
				String majorAbbrev = majorsCursor.getString(1);
				majors.add(new Major(majorName, majorAbbrev));
			} while (majorsCursor.moveToNext());
		}
		majorsCursor.close();
		
		return majors;
	}
	
	/**
     * getAllWorkAuths - gets a list of all the work authorizations
     * 
     * @param database - SQLite database object returned by ExternalDbOpenHelper.openDataBase
     * @return an ArrayList filled with the work authorizations
     */
	public static ArrayList<String> getAllWorkAuths(SQLiteDatabase database) {
		String[] empty = {};
		
		ArrayList<String> workAuths = new ArrayList<String>();
		Cursor workAuthsCursor = database.rawQuery("SELECT type FROM workAuth ORDER BY type;", empty);
		workAuthsCursor.moveToFirst();
		if (!workAuthsCursor.isAfterLast()) {
			do {
				workAuths.add(workAuthsCursor.getString(0));
			} while (workAuthsCursor.moveToNext());
		}
		workAuthsCursor.close();
		
		return workAuths;
	}
	
	/**
     * getAllPositions - gets a list of all the positions/employment types
     * 
     * @param database - SQLite database object returned by ExternalDbOpenHelper.openDataBase
     * @return an ArrayList filled with the positions
     */
	public static ArrayList<String> getAllPositions (SQLiteDatabase database) {
		String[] empty = {};
		
		ArrayList<String> positions = new ArrayList<String>();
		Cursor positionsCursor = database.rawQuery("SELECT type FROM employmentType ORDER BY type;", empty);
		positionsCursor.moveToFirst();
		if (!positionsCursor.isAfterLast()) {
			do {
				positions.add(positionsCursor.getString(0));
			} while (positionsCursor.moveToNext());
		}
		
		positionsCursor.close();
		
		return positions;
		
	}
	
	/**
	 * getTableCompanyMap
	 * 
	 * Returns a hash map keyed with the table number for all the companies in the wood or multipurpose gyms
	 * 
	 * @param WoodGym set to true to retrieve the wood gym companies, false to retrieve multipurpose room companies
	 * @param database - SQLite database object returned by ExternalDbOpenHelper.openDataBase
	 * @return a hash map with string table number keys to the company objects
	 */
	public static HashMap<String, Company> getTableCompanyMap(boolean WoodGym, SQLiteDatabase database) {
		HashMap<String, Company> map = new HashMap<String, Company>();
		String[] empty = {};
		
		String roomName;
		if (WoodGym) {
			roomName = "Wood";
		} else {
			roomName = "Multipurpose";
		}
		
		ArrayList<Company> companies = getAllCompanies(database);
		
		for (Company company : companies) {
			if (company.getRoom().equals(roomName)) {
				map.put(company.getTableNum(), company);
			}
		}
		
		return map;
	}
}	



package org.neo4j.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;
import spark.Spark;

import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Values;

import static spark.Spark.*;

public class SampleApp
{
    public static void main( String[] args )
    {
        Logger logger = LoggerFactory.getLogger(SampleApp.class);

        //            String uri = "bolt://@192.168.1.3:7687";
        String connectionString = System.getenv( "NEO4J_CONNECTION_STRING" );
        Driver driver = GraphDatabase.driver( connectionString );

        logger.warn("Connecting to: " + connectionString + " [" + driver + "]");

        get( "/users", users( driver ) );
        get( "/create-user", createUser( driver ) );

        Spark.exception( Exception.class, ( exception, request, response ) ->
        {
            exception.printStackTrace();
        } );
    }

    private static Route createUser( Driver driver )
    {
        return (req, res) ->
        {
            String bookmark;
            try ( Session session = driver.session( AccessMode.WRITE ) )
            {
                try(Transaction tx = session.beginTransaction()) {

                    tx.run( "CREATE (:User {screen_name: 'A.P. Cojones ' + rand()})" );
                    tx.success();
                }

                bookmark = session.lastBookmark();
            }
            return String.format( "User created [%s]", bookmark );
        };
    }

    private static Route users( Driver driver )
    {
        return ( req, res ) ->
        {
            StringBuilder builder = new StringBuilder();

            try ( Session session = driver.session( AccessMode.READ ) )
            {
                String query = "MATCH (u:User) RETURN u.screen_name AS screenName";

                StatementResult result = session.run( query );

                while ( result.hasNext() )
                {
                    Record row = result.next();
                    String screenName = row.get( "screenName" ).asString();
                    builder.append( screenName ).append( "<br />" );
                }
            }
            return builder.toString();
        };
    }
}

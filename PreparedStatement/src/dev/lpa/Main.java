package dev.lpa;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;

public class Main {

    private static final String INSERT_ARTIST = "INSERT INTO artists (artist_name) VALUES (?);";
    private static final String INSERT_ALBUM = "INSERT INTO albums (album_name, artist_id) VALUES (?, ?);";
    private static final String INSERT_SONGS = "INSERT INTO songs (track_number, song_title, album_id) VALUES (?, ?, ?)";

    public static void main(String[] args) {

        var dataSource = new MysqlDataSource();
        dataSource.setServerName("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabaseName("music");
        try {
            dataSource.setContinueBatchOnError(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try (Connection connection = dataSource.getConnection(System.getenv("USER"), System.getenv("PASS"))) {
            addDataFromFile(connection);

            String sql = "SELECT * FROM music.albumview WHERE artist_name = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, "Bob Dylan");
            printRecord(ps.executeQuery());
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void printRecord (ResultSet resultSet) {
        try {
            var metaData = resultSet.getMetaData();
            System.out.println("=====================");
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                System.out.printf("%-15s", metaData.getColumnName(i));
            }
            System.out.println();
            while(resultSet.next()) {
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    System.out.printf("%-15s", resultSet.getString(i));
                }
                System.out.println();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static int addArtist (PreparedStatement ps, Connection conn, String artistName)
    throws SQLException{

        int artistId = -1;
        ps.setString(1, artistName);
        int insertCount = ps.executeUpdate();

        if (insertCount > 0) {
            ResultSet generatedKey = ps.getGeneratedKeys();
            if (generatedKey.next()) {
                artistId = generatedKey.getInt(1);
                System.out.println("Auto-incremented ID: " + artistId);
            }
        }
        return artistId;
    }

    private static int addAlbum (PreparedStatement ps, Connection conn, String albumName, int artistId)
            throws SQLException{

        int albumId = -1;
        ps.setString(1, albumName);
        ps.setInt(2, artistId);
        int insertCount = ps.executeUpdate();

        if (insertCount > 0) {
            ResultSet generatedKey = ps.getGeneratedKeys();
            if (generatedKey.next()) {
                albumId = generatedKey.getInt(1);
                System.out.println("Auto-incremented ID: " + albumId);
            }
        }
        return albumId;
    }

    private static void addSong (PreparedStatement ps, Connection conn, int trackSong, String songTitle, int albumId)
            throws SQLException{

        ps.setInt(1, trackSong);
        ps.setString(2, songTitle);
        ps.setInt(3, albumId);

        ps.addBatch();
    }

    private static void addDataFromFile(Connection con)
        throws SQLException{

        List<String> records = null;
        try {
            records = Files.readAllLines(Path.of("NewAlbums.csv"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String lastAlbum = null;
        String lastArtist = null;
        int artistId = -1;
        int albumId = -1;
        try (
                PreparedStatement insertArtist = con.prepareStatement(INSERT_ARTIST, PreparedStatement.RETURN_GENERATED_KEYS);
                PreparedStatement insertAlbum = con.prepareStatement(INSERT_ALBUM, PreparedStatement.RETURN_GENERATED_KEYS);
                PreparedStatement insertSong = con.prepareStatement(INSERT_SONGS, PreparedStatement.RETURN_GENERATED_KEYS);
        ) {
            con.setAutoCommit(false);
            for (String record : records) {
                String[] columns = record.split(",");
                if (lastArtist ==null || !lastArtist.equals(columns[0])) {
                    lastArtist = columns[0];
                    artistId = addArtist(insertArtist, con, lastArtist);
                }
                if(lastAlbum == null ||  !lastAlbum.equals(columns[1])) {
                    lastAlbum = columns[1];
                    albumId = addAlbum(insertAlbum, con, lastAlbum, artistId);
                }
                addSong(insertSong, con, Integer.parseInt(columns[2]), columns[3], albumId);
            }
            int[] songsInserted = insertSong.executeBatch();
            System.out.println("%d songs recorded added %n".formatted(songsInserted.length));
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            con.rollback();
            throw new RuntimeException(e);
        }
    }

}

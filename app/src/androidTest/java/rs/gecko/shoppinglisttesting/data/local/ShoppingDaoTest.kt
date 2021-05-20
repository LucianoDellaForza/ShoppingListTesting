package rs.gecko.shoppinglisttesting.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import rs.gecko.shoppinglisttesting.getOrAwaitValue

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)  //required in androidTest, because its run on emulator and not on jvm
@SmallTest  //@SmallTests - unit tests, @MediumTest - integration tests, @LargeTests - Ui tests (Pyramid schema)
class ShoppingDaoTest {

    //this shit is needed whenever there is LiveData involved in any test
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: ShoppingItemDatabase
    private lateinit var dao: ShoppingDao

    @Before //before each test we want to create db in ram memory
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(    //it will only create db in RAM
            ApplicationProvider.getApplicationContext(),    //this is how to get context in androidTest directory
            ShoppingItemDatabase::class.java
        ).allowMainThreadQueries()  //to allow access to room db from main thread - in tests its important to execute all funs on main thread (sync)
            .build()
        dao = database.shoppingDao()
    }

    @After
    fun teardown() {
        database.close()    //close db after each test
    }

    //for some reason we take for granted that dao.observeAllShoppingItems() works correctly and use it for testing other methods

    @Test
    //dao funs are suspend, so we need to test them in coroutine, but we need to do in on main thread, so we use runBlockingTest {}
    //runBlocking and runBlockignTest will block main thread while executing
    //runBlockingTest will skip delay() functions inside of it, and some additional functions can be called (difference from runBlocking)
    fun insertShoppingItem() = runBlockingTest {
        //how to test insert? - insert item and then read that item from db
        val shoppingItem = ShoppingItem("testItem", 1, 1f, "url", id = 1)
        dao.insertShoppingItem(shoppingItem)

        //use already existing dao function to get all items
        val allShoppingItems = dao.observeAllShoppingItems()    //this returns LiveData and we need a value from it (for this we imported in a root dir of androidTest, a class from Google "LiveDataUtilAndroidTest.kt")
            //that has extenstion function getOrAwaitValue that gets a value from LiveData
            .getOrAwaitValue()
        //Google koristi ovaj metod u svojim primerima, ali
        //ne vidim razlog zasto nismo samo roknuli dao.observeAllShoppingItems().value nego morala dodatna klasa sa ext funkcijom

        //use Google's Truth library for assertThat()
        assertThat(allShoppingItems).contains(shoppingItem)
    }

    @Test
    fun deleteShoppingItem() = runBlockingTest {
        val shoppingItem = ShoppingItem("testItem", 1, 1f, "url", id = 1)
        dao.insertShoppingItem(shoppingItem)
        dao.deleteShoppingItem(shoppingItem)
        val allShoppingItems = dao.observeAllShoppingItems().getOrAwaitValue()

        assertThat(allShoppingItems).doesNotContain(shoppingItem)
    }

    @Test
    fun observeTotalPriceSum() = runBlockingTest {
        val shoppingItem1 = ShoppingItem("testItem1", 2, 10f, "url", id = 1)
        val shoppingItem2 = ShoppingItem("testItem2", 4, 5.5f, "url", id = 2)
        val shoppingItem3 = ShoppingItem("testItem3", 0, 100f, "url", id = 3)
        dao.insertShoppingItem(shoppingItem1)
        dao.insertShoppingItem(shoppingItem2)
        dao.insertShoppingItem(shoppingItem3)

        val totalPriceSum = dao.observeTotalPrice().getOrAwaitValue()

        assertThat(totalPriceSum).isEqualTo(2 * 10f + 4 * 5.5f)
    }
}
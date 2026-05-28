package com.example.herbhopper_v1.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.ui.admin.*
import com.example.herbhopper_v1.ui.admin.crud.ProductCrudScreen
import com.example.herbhopper_v1.ui.admin.AdminProfileScreen
import com.example.herbhopper_v1.ui.patient.*
import com.example.herbhopper_v1.ui.seller.*
import com.example.herbhopper_v1.viewmodel.CartViewModel
import com.example.herbhopper_v1.viewmodel.OrderViewModel
import com.example.herbhopper_v1.viewmodel.ProductViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    val cartViewModel: CartViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val orderViewModel: OrderViewModel = viewModel()
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val cartItems by cartViewModel.items.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(onTimeout = {
                navController.navigate("secure_access") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        // Patient Flow
        composable("secure_access") {
            SecureAccessScreen(
                onGoogleLogin = { navController.navigate("catalog") },
                onBiometricClick = { navController.navigate("login") },
                onPinClick = { navController.navigate("login") },
                onSignUpClick = { navController.navigate("create_account") },
                onForgotPasswordClick = { navController.navigate("forgot_password") },
                onAdminAccess = { navController.navigate("store_governance") },
                onSellerAccess = { navController.navigate("seller_dashboard") }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable("login") { 
            LoginScreen(
                onPatientSuccess = { navController.navigate("catalog") },
                onSellerSuccess = { navController.navigate("seller_dashboard") },
                onAdminSuccess = { navController.navigate("store_governance") },
                onForgotPasswordClick = { navController.navigate("forgot_password") },
                onSignUpClick = { navController.navigate("create_account") }
            ) 
        }
        composable("create_account") { 
            CreateAccountScreen(
                onSignUpSuccess = { navController.navigate("catalog") },
                onLoginClick = { navController.navigate("secure_access") }
            )
        }
        composable("home") { 
            MainMenuScreen(
                onLogout = { navController.navigate("secure_access") },
                onCatalogClick = { navController.navigate("catalog") },
                onOrdersClick = { navController.navigate("order_history") },
                onPrescriptionClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") },
                onHelpClick = { navController.navigate("help_center") }
            ) 
        }
        composable("catalog") { 
            CatalogScreen(
                productViewModel = productViewModel,
                onProductClick = { product ->
                    selectedProduct = product
                    navController.navigate("product_details")
                },
                onAddToCart = { cartViewModel.addToCart(it) },
                onMenuClick = { navController.navigate("home") },
                onCartClick = { navController.navigate("cart") },
                onHomeClick = { },
                onOrdersClick = { navController.navigate("order_history") },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") }
            )
        }
        composable("product_details") { 
            selectedProduct?.let { product ->
                ProductDetailsScreen(
                    product = product,
                    onBack = { navController.popBackStack() },
                    onAddToCart = { cartViewModel.addToCart(it) },
                    cartItemCount = cartItems.size,
                    onCartClick = { navController.navigate("cart") }
                )
            }
        }
        composable("prescription_validation") { 
            PrescriptionValidationScreen(
                onBack = { navController.popBackStack() },
                onHelpClick = { navController.navigate("help_center") }
            ) 
        }
        composable("cart") { 
            CartScreen(
                viewModel = cartViewModel,
                onBack = { navController.popBackStack() },
                onCheckout = { navController.navigate("checkout") }
            ) 
        }
        composable("checkout") {
            CheckoutScreen(
                totalAmount = cartItems.sumOf { it.product.price * it.quantity },
                onBack = { navController.popBackStack() },
                onPaymentSuccess = {
                    if (cartItems.isNotEmpty()) {
                        // 1. Convertir los items del carrito al formato simplificado de OrderItem
                        val orderItems = cartItems.map { cartItem ->
                            com.example.herbhopper_v1.data.OrderItem(
                                productId = cartItem.product.id,
                                productName = cartItem.product.name,
                                quantity = cartItem.quantity,
                                price = cartItem.product.price
                            )
                        }
                        val itemsJson = com.google.gson.Gson().toJson(orderItems)
                        
                        // 2. Construir la Orden
                        val orderId = "ORD-${(1000..9999).random()}-${System.currentTimeMillis() % 10000}"
                        val newOrder = com.example.herbhopper_v1.data.Order(
                            orderId = orderId,
                            userId = "david_g",
                            userName = "David G.",
                            itemsJson = itemsJson,
                            totalAmount = cartItems.sumOf { it.product.price * it.quantity },
                            status = "PENDING",
                            timestamp = System.currentTimeMillis(),
                            address = "Calle 10 # 5-12, Bogotá",
                            paymentMethod = "CREDIT_CARD"
                        )
                        
                        // 3. Persistir en la base de datos
                        orderViewModel.placeOrder(newOrder)
                    }

                    // 4. Limpiar el carrito y redirigir
                    cartViewModel.clearCart()
                    navController.navigate("order_history") {
                        popUpTo("cart") { inclusive = true }
                    }
                }
            )
        }
        composable("order_history") { 
            OrderHistoryScreen(
                viewModel = orderViewModel,
                onHomeClick = { navController.navigate("catalog") },
                onOrdersClick = { },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") },
                onCartClick = { navController.navigate("cart") }
            )
        }
        composable("order_tracking") { 
            OrderTrackingScreen(
                onHomeClick = { navController.navigate("catalog") },
                onOrdersClick = { navController.navigate("order_history") },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") },
                onCartClick = { navController.navigate("cart") }
            )
        }
        composable("profile") {
            ProfileScreen(
                onHomeClick = { navController.navigate("catalog") },
                onOrdersClick = { navController.navigate("order_history") },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onLogout = { navController.navigate("secure_access") },
                onOptionClick = { option -> navController.navigate("profile_settings/$option") }
            )
        }
        composable(
            "profile_settings/{option}",
            arguments = listOf(navArgument("option") { type = NavType.StringType })
        ) { backStackEntry ->
            val option = backStackEntry.arguments?.getString("option") ?: ""
            ProfileSettingsScreen(title = option, onBack = { navController.popBackStack() })
        }
        composable("help_center") {
            HelpCenterScreen(
                onBack = { navController.popBackStack() },
                onChatClick = { navController.navigate("chat") }
            )
        }
        composable("chat") {
            ChatScreen(onBack = { navController.popBackStack() })
        }

        // Seller Flow
        composable("seller_dashboard") { 
            SellerDashboardScreen(
                viewModel = productViewModel,
                onNavigate = { navController.navigate(it) }
            ) 
        }
        composable("inventory") { 
            InventoryScreen(
                viewModel = productViewModel,
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                onAddProduct = { navController.navigate("inventory_crud") },
                onEditProduct = { productId -> navController.navigate("inventory_crud/$productId") }
            ) 
        }
        composable("inventory_crud") {
            InventoryCrudScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "inventory_crud/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId")
            InventoryCrudScreen(
                productId = productId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("incoming_orders") { 
            IncomingOrdersScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                viewModel = orderViewModel
            ) 
        }
        composable(
            "order_details/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailsScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() },
                viewModel = orderViewModel
            )
        }
        composable("store_profile") { 
            StoreProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                onLogout = { navController.navigate("secure_access") },
                onOptionClick = { option -> navController.navigate("profile_settings/$option") }
            ) 
        }

        // Admin Flow
        composable("store_governance") { 
            StoreGovernanceScreen(
                onNavigate = { navController.navigate(it) },
                onLogout = { navController.navigate("secure_access") }
            )
        }
        composable("admin_profile") {
            AdminProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                onLogout = { navController.navigate("secure_access") }
            )
        }
        composable("user_management") {
            UserManagementScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) }
            )
        }
        composable("audit_panel") { 
            AuditPanelScreen(onNavigate = { navController.navigate(it) }) 
        }
        composable("global_analytics") { 
            GlobalAnalyticsScreen(onNavigate = { navController.navigate(it) }) 
        }
        composable("system_infrastructure") { 
            SystemInfrastructureScreen(onNavigate = { navController.navigate(it) }) 
        }
        composable("product_crud") { ProductCrudScreen() }
    }
}
